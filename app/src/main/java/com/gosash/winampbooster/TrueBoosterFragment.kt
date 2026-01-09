package com.gosash.winampbooster

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class TrueBoosterFragment : Fragment(R.layout.fragment_true_booster) {

    private lateinit var engine: PcmBoostPlayer
    private val ui = Handler(Looper.getMainLooper())

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            engine.load(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ThemePrefs.applyToView(view)
        engine = PcmBoostPlayer(requireContext())

        val txtGain = view.findViewById<TextView>(R.id.txtGain)
        val seekGain = view.findViewById<SeekBar>(R.id.seekGain)
        val chkLimiter = view.findViewById<CheckBox>(R.id.chkLimiter)
        val txtPos = view.findViewById<TextView>(R.id.txtPos)

        view.findViewById<Button>(R.id.btnPickAudio).setOnClickListener {
            pickAudio.launch(arrayOf("audio/*"))
        }
        view.findViewById<Button>(R.id.btnPlay).setOnClickListener { engine.play() }
        view.findViewById<Button>(R.id.btnPause).setOnClickListener { engine.pause() }
        view.findViewById<Button>(R.id.btnStop).setOnClickListener { engine.stop() }

        seekGain.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                txtGain.text = "Gain: $p dB"
                engine.setGainDb(p.toFloat())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        chkLimiter.setOnCheckedChangeListener { _, isChecked ->
            engine.setLimiterEnabled(isChecked)
        }

        fun tick() {
            val pos = engine.positionMs() / 1000
            val dur = engine.durationMs() / 1000
            txtPos.text = "%02d:%02d / %02d:%02d".format(pos / 60, pos % 60, dur / 60, dur % 60)
            ui.postDelayed({ tick() }, 300)
        }
        tick()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        engine.release()
    }
}
