package com.gosash.winampbooster

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlayerFragment : Fragment(R.layout.fragment_player) {

    private var player: ExoPlayer? = null
    private lateinit var urlBox: EditText
    private lateinit var favAdapter: FavAdapter

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { playUri(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ThemePrefs.applyToView(view)

        urlBox = view.findViewById(R.id.urlBox)
        val playerView = view.findViewById<PlayerView>(R.id.playerView)
        val favList = view.findViewById<RecyclerView>(R.id.favList)

        player = ExoPlayer.Builder(requireContext()).build().also { exo ->
            playerView.player = exo
        }

        view.findViewById<Button>(R.id.btnPlayUrl).setOnClickListener {
            val url = urlBox.text.toString().trim()
            if (url.isNotEmpty()) playUrl(url)
        }

        view.findViewById<Button>(R.id.btnPickFile).setOnClickListener {
            pickFile.launch(arrayOf("audio/*", "video/*"))
        }

        view.findViewById<Button>(R.id.btnSaveFav).setOnClickListener {
            val url = urlBox.text.toString().trim()
            if (url.isNotEmpty()) {
                FavStore.add(requireContext(), url)
                favAdapter.submit(FavStore.getAll(requireContext()))
            }
        }

        favAdapter = FavAdapter(
            onPlay = { playUrl(it) },
            onDelete = {
                FavStore.remove(requireContext(), it)
                favAdapter.submit(FavStore.getAll(requireContext()))
            }
        )

        favList.layoutManager = LinearLayoutManager(requireContext())
        favList.adapter = favAdapter
        favAdapter.submit(FavStore.getAll(requireContext()))
    }

    private fun playUrl(url: String) {
        val item = MediaItem.fromUri(Uri.parse(url))
        player?.setMediaItem(item)
        player?.prepare()
        player?.play()
    }

    private fun playUri(uri: Uri) {
        val item = MediaItem.fromUri(uri)
        player?.setMediaItem(item)
        player?.prepare()
        player?.play()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
    }
}
