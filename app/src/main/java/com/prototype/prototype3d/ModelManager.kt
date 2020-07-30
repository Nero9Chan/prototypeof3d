package com.prototype.prototype3d

import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import java.util.concurrent.CompletableFuture
import kotlin.math.pow
import kotlin.math.sqrt

class ModelManager {
    private var scene: Scene
    private var activity: Activity

    private lateinit var renderableModel: ModelRenderable
    private lateinit var renderableFuture: CompletableFuture<ModelRenderable>
    private lateinit var materialFuture: CompletableFuture<CustomMaterial>
    private lateinit var customMaterial: CustomMaterial

    private var originalX: Float = 0f
    private var originalY: Float = 0f
    var deltaX: Float = 0f
    var deltaY: Float = 0f
    private var pressTime: Long = 0
    private var rotated: Boolean = false
    private val rotationTrigger: Float = 30f
    private val rotationSpeed: Float = 0.005f
    private val rotationRatio: Float = 1.1f
    private var rotationLatch: Boolean = true
    private var timerLatch: Boolean = false
    //^^^ for rotation

    private var originalDistance: Float = -1f
    private var deltaDistance: Float = 0f
    private var distanceLatch: Boolean = false
    private var oldDeltaDistance: Float = 0f
    private val deltaZoomTrigger: Float = 3f //sensitive
    private val scaling: Float = 1.015f
    private var oldPointer0x: Float = -1f
    private var oldPointer0y: Float = -1f
    private var oldPointer1x: Float = -1f
    private var oldPointer1y: Float = -1f
    //^^^ for zooming


    private val zoomingNoise: Float = 0.01f //in zooming meeting 0.1 was used
    private var zoomingLatch: Boolean = true
    private var movingLactch: Boolean = true
    //^^^ for both zooming and moving

    private val movingNoise: Float = 1.0f
    private val movingSpeed: Float = 0.045f//0.035 0.003
    //^^^ for moving
    //^^^^ for OnTouchListener

    private var size: Int = 0
    private var nodeArr: Array<Node>
    private var renderableFutureArr: Array<CompletableFuture<ModelRenderable>?>

    private var currentIndex: Int = 0
    private var selectedIndex: Int = 0
    private var selectedName: String = ""


    constructor(activity: Activity, scene: Scene, size: Int) {
        Log.d("checking", activity.toString())
        this.activity = activity
        this.scene = scene
        this.size = size
        nodeArr = Array<Node>(size){Node()}
        renderableFutureArr = Array<CompletableFuture<ModelRenderable>?>(size){null}
    }

    fun createNode(
        name: String,
        uri: String,
        render: Boolean,
        position: Vector3,
        scale: Vector3,
        rotation: Quaternion = Quaternion.axisAngle(Vector3(0f, 0f, 0f), 0f)
    ) : Node {
        renderableFuture = ModelRenderable.builder()
            .setSource(activity, Uri.parse(uri))
            .build()

        renderableFuture.thenAccept { addNode(it, name, render, position, scale, rotation) }

        /*Log.d("checking", "function index 0: " + nodeArr[0].toString())
        Log.d("checking", "function index 1: " + nodeArr[1].toString())
        Log.d("checking", "function index 2: " + nodeArr[2].toString())
        Log.d("checking", "function index 2: " + nodeArr[3].toString())
        Log.d("checking", "function index 2: " + nodeArr[4].toString())
        Log.d("checking", "current index: " + (currentIndex).toString())*/
        return nodeArr[currentIndex]
    }

    fun getRenderableFuture(): CompletableFuture<ModelRenderable>{
        return renderableFuture
    }

    fun getNode(index: Int): Node{
        return nodeArr[index]
    }

    fun getSelectedIndex(): Int{
        return selectedIndex
    }

    fun selectNext(){
        if(selectedIndex == currentIndex-1)
            selectedIndex = 0
        else
            selectedIndex++
    }

    private fun addNode(
        model: ModelRenderable,
        name: String,
        render: Boolean,
        position: Vector3,
        scale: Vector3,
        rotation: Quaternion
    ) {
        model?.let {
            nodeArr[currentIndex].apply {
                setParent(scene)
                localPosition = position
                localScale = scale
                renderable = it
                localRotation = rotation
                this.name = name
            }
            //setOnTouchListener
            setNewOnTouchListening(nodeArr[currentIndex])
            if(render){
                //scene.addChild(nodeArr[currentIndex])
                nodeArr[currentIndex].setParent(scene)
                if(nodeArr[currentIndex].parent != null)
                    Log.d("checking",nodeArr[currentIndex].parent.name )
                else
                    Log.d("checking", "null" )
            }
        }
        Log.d("checking", "AFTER FUNCTION index:" + currentIndex + "  " + nodeArr[currentIndex].toString())
        //reserved

        currentIndex++
    }

    fun setSelectedName(node: Node){
        selectedName = node.name
    }

    fun getCurrentIndex() :Int{
        return currentIndex
    }


    private fun setNewOnTouchListening(currentNode: Node) {
        currentNode.setOnTouchListener { hitTestResult, motionEvent ->

            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    if(selectedName == "")
                        selectedName = currentNode.name
                    pressTime = motionEvent.eventTime
                    Log.d("checking", pressTime.toString())
                    originalX = motionEvent.x
                    originalY = motionEvent.y
                }
                MotionEvent.ACTION_UP -> {
                    rotationLatch = true
                    zoomingLatch = true
                    movingLactch = true  //^^^ unlock/reset all latch
                    timerLatch = false //reset timerLatch
                    selectedName = ""
                    Log.d("checking", currentNode.localRotation.toString())
                }
                MotionEvent.ACTION_MOVE -> {
                    //Log.d("checking", motionEvent.pointerCount.toString())
                    if (SystemClock.uptimeMillis() - pressTime > 100) {
                        timerLatch =
                            true //if there is ONLY 1 finger after 100ms (0.1s), then rotation (1 finger) is enabled
                    }                     //if second finger is touching the model within 100ms, it considers as 2 fingers gesture and disable rotation (1 finger gesture)
                    //basically it is a buffer time for using 2-finger gesture
                    //otherwise, 1 finger rotation is easily misleded

                    if (motionEvent.pointerCount == 1 && rotationLatch && timerLatch && selectedName == currentNode.name) {
                        deltaX = originalX - motionEvent.x
                        deltaY = originalY - motionEvent.y

                        if (deltaX > rotationTrigger) { //finger dragging to left
                            currentNode.localRotation = Quaternion.multiply(
                                currentNode.localRotation,
                                Quaternion(
                                    Vector3.down(),
                                    rotationSpeed * (deltaX.pow(rotationRatio))
                                )
                            )

                            //Log.d("checking", (rotationSpeed * (deltaX.pow(rotationRatio))).toString())
                            rotated = true;
                        } else if (deltaX < -rotationTrigger) { //finger dragging to right
                            currentNode.localRotation = Quaternion.multiply(
                                currentNode.localRotation,
                                Quaternion(
                                    Vector3.up(),
                                    rotationSpeed * ((-deltaX).pow(rotationRatio))
                                )
                            )

                            //Log.d("checking", (rotationSpeed * ((-deltaX).pow(rotationRatio))).toString())
                            rotated = true;
                        }

                        if (deltaY > rotationTrigger) { //finger dragging to up
                            currentNode.localRotation = Quaternion.multiply(
                                currentNode.localRotation,
                                Quaternion(
                                    Vector3.left(),
                                    rotationSpeed * (deltaY.pow(rotationRatio))
                                )
                            )

                            //Log.d("checking", (rotationSpeed * ((deltaY).pow(rotationRatio))).toString())
                            rotated = true;
                        } else if (deltaY < -rotationTrigger) { //finger dragging to down
                            currentNode.localRotation = Quaternion.multiply(
                                currentNode.localRotation,
                                Quaternion(
                                    Vector3.right(),
                                    rotationSpeed * ((-deltaY).pow(rotationRatio))
                                )
                            )

                            //Log.d("checking", (rotationSpeed * ((-deltaY).pow(rotationRatio))).toString())
                            rotated = true;
                        }
                    } else if (motionEvent.pointerCount == 2 && selectedName == currentNode.name) {
                        /*Log.d("checking", "Pointer index 0 X: " + motionEvent.getX(0).toString())
                        Log.d("checking", "Pointer index 0 Y: " + motionEvent.getY(0).toString())
                        Log.d("checking", "Pointer index 1 X: " + motionEvent.getX(1).toString())
                        Log.d("checking", "Pointer index 1 Y: " + motionEvent.getY(1).toString())*/

                        rotationLatch = false

                        if (!distanceLatch) {
                            originalDistance = sqrt(
                                (motionEvent.getX(0) - motionEvent.getX(1)).pow(2) +
                                        (motionEvent.getY(0) - motionEvent.getY(1)).pow(2)
                            )
                            distanceLatch = true
                        }

                        deltaDistance = sqrt(
                            (motionEvent.getX(0) - motionEvent.getX(1)).pow(2) +
                                    (motionEvent.getY(0) - motionEvent.getY(1)).pow(2)
                        ) - originalDistance

                        if (oldDeltaDistance == -1f)// 0 means oldDeltaDistance has not really been initialized yet
                            oldDeltaDistance = deltaDistance

                        if (oldPointer0x == -1f || oldPointer0y == -1f || oldPointer1x == -1f || oldPointer1y == -1f) {
                            oldPointer0x = motionEvent.getX(0)
                            oldPointer0y = motionEvent.getY(0)
                            oldPointer1x = motionEvent.getX(1)
                            oldPointer1y = motionEvent.getY(1)
                        }

                        /*Log.d("checking", "0x " + (motionEvent.getX(0) - oldPointer0x).toString())
                        Log.d("checking", "1x " + (motionEvent.getX(1) - oldPointer1x).toString())
                        Log.d("checking", "0y " + (motionEvent.getY(0) - oldPointer0y).toString())
                        Log.d("checking", "1y " + (motionEvent.getY(1) - oldPointer1y).toString())*/

                        if (zoomingLatch
                            && !((motionEvent.getX(0) - oldPointer0x) > zoomingNoise && (motionEvent.getX(
                                1
                            ) - oldPointer1x) > zoomingNoise)
                            && !((motionEvent.getY(0) - oldPointer0y) > zoomingNoise && (motionEvent.getY(
                                1
                            ) - oldPointer1y) > zoomingNoise)
                            && !((motionEvent.getX(0) - oldPointer0x) < -zoomingNoise && (motionEvent.getX(
                                1
                            ) - oldPointer1x) < -zoomingNoise)
                            && !((motionEvent.getY(0) - oldPointer0y) < -zoomingNoise && (motionEvent.getY(
                                1
                            ) - oldPointer1y) < -zoomingNoise)
                        ) {
                            movingLactch = false

                            if (deltaDistance - oldDeltaDistance < -deltaZoomTrigger) {
                                //Log.d( "checking", "new fea " + (deltaDistance - oldDeltaDistance).toString() )
                                currentNode.localScale = currentNode.localScale.scaled(1 / scaling)
                                //Log.d("checking", "new scale " + currentNode.localScale)
                            } else if (deltaDistance - oldDeltaDistance > deltaZoomTrigger) {
                                //Log.d("checking", "new fea " + (deltaDistance - oldDeltaDistance).toString())
                                currentNode.localScale = currentNode.localScale.scaled(scaling)
                                //Log.d("checking", "new scale " + currentNode.localScale)
                            }
                        } else if (movingLactch) {
                            if ((motionEvent.getX(0) - oldPointer0x) > movingNoise && (motionEvent.getX(
                                    1
                                ) - oldPointer1x) > movingNoise
                            ) { //moving right
                                currentNode.localPosition = Vector3.add(
                                    currentNode.localPosition,
                                    Vector3(movingSpeed, 0f, 0f)
                                )
                                zoomingLatch = false
                            }

                            if ((motionEvent.getX(0) - oldPointer0x) < -movingNoise && (motionEvent.getX(
                                    1
                                ) - oldPointer1x) < -movingNoise
                            ) { //moving left
                                currentNode.localPosition = Vector3.add(
                                    currentNode.localPosition,
                                    Vector3(-movingSpeed, 0f, 0f)
                                )
                                zoomingLatch = false
                            }

                            if ((motionEvent.getY(0) - oldPointer0y) > movingNoise && (motionEvent.getY(
                                    1
                                ) - oldPointer1y) > movingNoise
                            ) { //moving up
                                currentNode.localPosition = Vector3.add(
                                    currentNode.localPosition,
                                    Vector3(0f, -movingSpeed, 0f)
                                )
                                zoomingLatch = false
                            }

                            if ((motionEvent.getY(0) - oldPointer0y) < -movingNoise && (motionEvent.getY(
                                    1
                                ) - oldPointer1y) < -movingNoise
                            ) {//moving down
                                currentNode.localPosition = Vector3.add(
                                    currentNode.localPosition,
                                    Vector3(0f, movingSpeed, 0f)
                                )
                                zoomingLatch = false
                            }
                        }


                        oldPointer0x = motionEvent.getX(0)
                        oldPointer0y = motionEvent.getY(0)
                        oldPointer1x = motionEvent.getX(1)
                        oldPointer1y = motionEvent.getY(1)
                        oldDeltaDistance = deltaDistance
                    }
                }
            }
            true
        }
    }
}