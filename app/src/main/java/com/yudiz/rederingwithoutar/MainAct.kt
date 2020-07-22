package com.yudiz.rederingwithoutar

//vvv added this two

import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.RotationController
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
//import com.sun.xml.internal.ws.addressing.EndpointReferenceUtil.transform
import kotlinx.android.synthetic.main.act_main.*
import java.util.concurrent.CompletableFuture


class MainAct : AppCompatActivity() {

    private lateinit var scene: Scene

    private lateinit var renderableModel: ModelRenderable
    private lateinit var renderableFuture: CompletableFuture<ModelRenderable>
    private lateinit var materialFuture: CompletableFuture<CustomMaterial>
    private lateinit var customMaterial: CustomMaterial

    private lateinit var currentNode: Node

    private lateinit var colorButtonText: String
    private lateinit var metallicButtonText: String
    private lateinit var roughnessButtonText: String
    private lateinit var normalButtonText: String
    private lateinit var resetMaterialButtonText: String

    private var originalX: Float = 0f
    private var originalY: Float = 0f
    private var pressTime: Long = 0
    private var releaseTime: Long = 0
    private var scalingTimeMs: Long = 3000 //3 seconds
    private var small: Boolean = true
    private var rotated: Boolean = false
    //^^^^ for OnTouchListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_main)

        scene = sceneView.scene

        materialFuture = CustomMaterial.build(this) {
            baseColorSource = Uri.parse("textures/cube_diffuse.jpg")
            metallicSource = Uri.parse("textures/cube_metallic.jpg")
            roughnessSource = Uri.parse("textures/cube_roughness.jpg")
            normalSource = Uri.parse("textures/cube_normal.jpg")
        }

        renderCube()

        colorButton.setOnClickListener {
            customMaterial.switchBaseColor()
            if (customMaterial.isDefaultBaseColorMap) {
                colorButton.setText(R.string.set_color)
            } else
                colorButton.setText(R.string.reset_color)
        }

        metallicButton.setOnClickListener {
            customMaterial.switchMetallic()
            if (customMaterial.isDefaultMetallicMap)
                metallicButton.setText(R.string.set_metallic)
            else
                metallicButton.setText(R.string.reset_metallic)
        }

        roughnessButton.setOnClickListener {
            customMaterial.switchRoughness()
            if (customMaterial.isDefaultRoughnessMap)
                roughnessButton.setText(R.string.set_roughness)
            else
                roughnessButton.setText(R.string.reset_roughness)
        }

        normalButton.setOnClickListener {
            customMaterial.switchNormal()
            if (customMaterial.isDefaultNormalMap)
                normalButton.setText(R.string.set_normal)
            else
                normalButton.setText(R.string.reset_normal)
        }

        resetMaterialButton.setOnClickListener {
            customMaterial.reset()
            colorButton.setText(R.string.set_color)
            metallicButton.setText(R.string.set_metallic)
            roughnessButton.setText(R.string.set_roughness)
            normalButton.setText(R.string.set_normal)
        }

        changeModelButton.setOnClickListener {
            if (currentNode.name == "cube")
                renderCup()
            else
                renderCube()
        }
    }

    private fun renderCube(){
        renderableFuture = ModelRenderable.builder()
            .setSource(this, Uri.parse("cube.sfb"))
            .build()

        if(::currentNode.isInitialized)
            scene.removeChild(currentNode)

        renderableFuture.thenAccept { addNodeCube(it) }

        renderableFuture.thenAcceptBoth(materialFuture) { renderableResult, materialResult ->
            customMaterial = materialResult
            renderableModel = renderableResult
            renderableModel.material = customMaterial.value
        }

        //enableButton()
    }

    //for cup
    private fun renderCup(){
        renderableFuture = ModelRenderable.builder()
            .setSource(this, Uri.parse("coffee_cup.sfb"))
            .build()

        if(::currentNode.isInitialized)
            scene.removeChild(currentNode)

        renderableFuture.thenAccept { addNodeCup(it) }

        //disableButton()
    }

    //for cube
    private fun addNodeCube(model: ModelRenderable?) {
        Log.d("checking", model.toString())

        small = true

        model?.let {
            currentNode = Node().apply {
                setParent(scene)
                localPosition = Vector3(0f, -2f, -5f)
                localScale = Vector3(4f, 4f, 4f)
                renderable = it
                localRotation = Quaternion.axisAngle(Vector3(1f, -3.5f, 0f), 10f)
                name = "cube"
            }
            //setOnTouchListener
            setNewOnTouchListening(currentNode)
            scene.addChild(currentNode)
        }
    }

    //for cup
    private fun addNodeCup(model: ModelRenderable?) {
        Log.d("checking", model.toString())

        small = true

        model?.let {
            currentNode = Node().apply {
                setParent(scene)
                localPosition = Vector3(0f, -2f, -7f)
                localScale = Vector3(3f, 3f, 3f)
                renderable = it
                name = "cup"
            }
            //setOnTouchListener
            setNewOnTouchListening(currentNode)
            scene.addChild(currentNode)
        }
    }

    private fun disableButton() {
        colorButton.isEnabled = false
        metallicButton.isEnabled = false
        roughnessButton.isEnabled = false
        normalButton.isEnabled = false
        resetMaterialButton.isEnabled = false


        colorButton.isClickable = false
        metallicButton.isClickable = false
        roughnessButton.isClickable = false
        normalButton.isClickable = false
        resetMaterialButton.isClickable = false


        colorButtonText = colorButton.text.toString()
        metallicButtonText = metallicButton.text.toString()
        roughnessButtonText = roughnessButton.text.toString()
        normalButtonText = normalButton.text.toString()
        resetMaterialButtonText = resetMaterialButton.text.toString()

        colorButton.text = ""
        metallicButton.text = ""
        roughnessButton.text = ""
        normalButton.text = ""
        resetMaterialButton.text = ""
    }

    private fun enableButton() {
        colorButton.isEnabled = true
        metallicButton.isEnabled = true
        roughnessButton.isEnabled = true
        normalButton.isEnabled = true
        resetMaterialButton.isEnabled = true

        colorButton.isClickable = true
        metallicButton.isClickable = true
        roughnessButton.isClickable = true
        normalButton.isClickable = true
        resetMaterialButton.isClickable = true

        colorButton.text = colorButtonText
        metallicButton.text = metallicButtonText
        roughnessButton.text = roughnessButtonText
        normalButton.text = normalButtonText
        resetMaterialButton.text = resetMaterialButtonText
    }

    private fun setNewOnTouchListening(currentNode: Node){
        currentNode.setOnTouchListener { hitTestResult, motionEvent ->
            //Log.d("checking",  "trying drag")
            var deltaX: Float
            var deltaY: Float


            when(motionEvent.action){
                MotionEvent.ACTION_DOWN -> {
                    pressTime = motionEvent.eventTime
                    Log.d("checking", pressTime.toString())
                    originalX = motionEvent.x
                    originalY = motionEvent.y
                }
                MotionEvent.ACTION_UP -> {
                    releaseTime = SystemClock.uptimeMillis()
                    if((releaseTime - pressTime) > scalingTimeMs && !rotated){
                        if(currentNode.name == "cube"){
                            if(small)
                                currentNode.localScale = Vector3(6f,6f,6f)
                            else
                                currentNode.localScale = Vector3(4f,4f,4f)
                        }
                        else if(currentNode.name == "cup"){
                            if(small)
                                currentNode.localScale = Vector3(5f,5f,5f)
                            else
                                currentNode.localScale = Vector3(3f,3f,3f)
                        }
                        small = !small
                    }
                    rotated = false
                }
                MotionEvent.ACTION_MOVE -> {
                    deltaX = originalX - motionEvent.x
                    deltaY = originalY - motionEvent.y
                    var rotationSpeed: Float = 0.5f
                    var rotationTrigger: Float = 30f

                    if(deltaX > rotationTrigger){
                        currentNode.localRotation = Quaternion.multiply(currentNode.localRotation, Quaternion(Vector3.down(), rotationSpeed))
                        rotated = true;
                    }

                    else if(deltaX < -rotationTrigger){
                        currentNode.localRotation = Quaternion.multiply(currentNode.localRotation, Quaternion(Vector3.up(), rotationSpeed))
                        rotated = true;
                    }

                    if(deltaY > rotationTrigger){
                        currentNode.localRotation = Quaternion.multiply(currentNode.localRotation, Quaternion(Vector3.left(), rotationSpeed))
                        rotated = true;
                    }

                    else if(deltaY < -rotationTrigger){
                        currentNode.localRotation = Quaternion.multiply(currentNode.localRotation, Quaternion(Vector3.right(), rotationSpeed))
                        rotated = true;
                    }
                }
            }
            true
        }
    }

    override fun onPause() {
        super.onPause()
        sceneView.pause()
    }

    override fun onResume() {
        super.onResume()
        sceneView.resume()
    }
}


