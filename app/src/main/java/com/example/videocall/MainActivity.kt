package com.example.videocall

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.videocall.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val appId = "db390dfd079044c884e5c26d1033de94 "
    private val ChannelName = "abhishek"
    private val token =
        "007eJxTYHhwQGa6isTOw/wMLloGb8u/3ZG6xH+qfB+rmdPUefszDgcoMKQkGVsapKSlGJhbGpiYJFtYmKSaJhuZpRgaGBunpFqaMDl/SWkIZGRgO+XBwsgAgSA+B0NiUkZmcUZqNgMDAGlhH1A="
    private var uid = 0
    private var isJoined = false
    private var agoraEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null


    private val PERMISSION_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA
    )

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSIONS[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    REQUESTED_PERMISSIONS[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }

    private fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.message)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!checkSelfPermission()) {
            ActivityCompat
                .requestPermissions(
                    this, REQUESTED_PERMISSIONS, PERMISSION_ID
                )
        }
        setupVideoSDKEngine()



        binding.JoinButton.setOnClickListener {
            joinCall()
        }
        binding.LeaveButton.setOnClickListener {
            leaveCall()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        // Destroy the engine in a sub-thread to avoid congestion
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }


    private fun leaveCall() {
        if (!isJoined) {
            showMessage("join a channel first")
        } else {
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = GONE
            if (localSurfaceView != null) localSurfaceView!!.visibility = GONE
            isJoined = false


        }


    }

    private fun joinCall() {

        if (checkSelfPermission()) {
            val option = ChannelMediaOptions()
            option.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            option.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setupLocalVideo()
            localSurfaceView!!.visibility = VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token, ChannelName, uid, option)
        } else {
            Toast.makeText(this, "permission not granted", Toast.LENGTH_SHORT).show()


        }


    }

    private val mRtcEventHandler: IRtcEngineEventHandler =
        object : IRtcEngineEventHandler() {
            override fun onUserJoined(uid: Int, elapsed: Int) {
                showMessage("Remote User Joined $uid")
                runOnUiThread { setupRemoteVideo(uid) }
            }

            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                isJoined = true
                showMessage("Joined Channel $channel")
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                showMessage("user offline")
                runOnUiThread {
                    remoteSurfaceView!!.visibility = GONE

                }


            }

        }

    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        binding.remoteVideoViewContainer.addView(remoteSurfaceView)
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
    }

    private fun setupLocalVideo() {
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = SurfaceView(baseContext)
        binding.localVideoViewContainer.addView(localSurfaceView)
        // Call setupLocalVideo with a VideoCanvas having uid set to 0.
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }


}