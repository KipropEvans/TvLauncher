package com.tv.applelauncher

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tv.applelauncher.adapters.ShelfAdapter
import com.tv.applelauncher.data.ProfileManager
import com.tv.applelauncher.models.ContentItem
import com.tv.applelauncher.models.Shelf
import com.tv.applelauncher.streaming.JellyfinApi
import com.tv.applelauncher.streaming.JellyfinSession
import com.tv.applelauncher.streaming.JfItem
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var shelves: RecyclerView
    private lateinit var heroImage: ImageView
    private lateinit var heroTitle: TextView
    private lateinit var heroMeta: TextView
    private lateinit var background: ImageView
    private lateinit var clock: TextView

    private var heroItems = listOf<ContentItem>()
    private var heroIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        val TABS = listOf("Watch Now", "Store", "Library", "Search")
    }
    private var currentTab = 0

    private val heroRotator = object : Runnable {
        override fun run() {
            if (heroItems.isNotEmpty()) {
                showHero((heroIndex + 1) % heroItems.size)
                handler.postDelayed(this, 8000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        shelves = findViewById(R.id.shelves)
        heroImage = findViewById(R.id.hero_image)
        heroTitle = findViewById(R.id.hero_title)
        heroMeta = findViewById(R.id.hero_meta)
        background = findViewById(R.id.background)
        clock = findViewById(R.id.clock)

        startClock()
        applyBlurBackground()
        setupTopBar()
        loadContent()
    }

    // ---------- Top bar ----------
    private fun startClock() {
        val fmt = SimpleDateFormat("EEE d MMM • h:mm a", Locale.getDefault())
        val tick = object : Runnable {
            override fun run() {
                clock.text = "fmt.format(Date())•{fmt.format(Date())}   •fmt.format(Date())•{ProfileManager.getCurrent(this@MainActivity).name}"
                handler.postDelayed(this, 30_000)
            }
        }
        tick.run()
    }

    private fun applyBlurBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            background.setRenderEffect(
                RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP))
        }
        background.alpha = 0.35f
    }

    private fun setupTopBar() {
        walkTabs(window.decorView) { tv, index ->
            tv.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && index != currentTab) {
                    currentTab = index
                    refreshTabs()
                    when (TABS[index]) {
                        "Store" -> startActivity(Intent(this, StoreActivity::class.java))
                        "Library" -> startActivity(Intent(this, LibraryActivity::class.java))
                        "Search" -> startActivity(Intent(this, SearchActivity::class.java))
                    }
                }
            }
        }
        refreshTabs()

        findViewById<ImageView>(R.id.profile).apply {
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ProfileSwitcherActivity::class.java))
            }
            setOnLongClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java)); true
            }
        }
    }

    private fun walkTabs(v: View, action: (TextView, Int) -> Unit) {
        if (v is TextView && v.text.toString() in TABS) action(v, TABS.indexOf(v.text.toString()))
        if (v is ViewGroup) for (i in 0 until v.childCount) walkTabs(v.getChildAt(i), action)
    }

    private fun refreshTabs() {
        walkTabs(window.decorView) { tv, index ->
            tv.setTextColor(resources.getColor(
                if (index == currentTab) R.color.white else R.color.tab_inactive, theme))
            tv.paint.isFakeBoldText = index == currentTab
        }
    }

    // ---------- Content ----------
    private fun loadContent() {
        scope.launch {
            JellyfinSession.load(this@MainActivity)

            if (!JellyfinSession.isConnected()) {
                setupFallback()
                return@launch
            }

            try {
                val api = withContext(Dispatchers.IO) {
                    JellyfinApi.create(JellyfinSession.serverUrl, JellyfinSession.accessToken)
                }
                val uid = JellyfinSession.userId

                val (resume, latest, libraries) = coroutineScope {
                    val r = async(Dispatchers.IO) { api.getResume(uid).Items }
                    val l = async(Dispatchers.IO) { api.getLatest(uid) }
                    val libs = async(Dispatchers.IO) { api.getLibraries(uid).Items }
                    Triple(r.await(), l.await(), libs.await())
                }

                fun toContent(items: List<JfItem>) = items.map { jf ->
                    ContentItem(
                        id = jf.Id.hashCode().toLong(),
                        title = jf.SeriesName ?: jf.Name,
                        subtitle = when {
                            jf.Type == "Episode" ->
                                "Sjf.ParentIndexNumber:E{jf.ParentIndexNumber}:Ejf.ParentIndexNumber:E{jf.IndexNumber} • ${jf.Name}"
                            else -> listOfNotNull(
                                jf.ProductionYear?.toString(),
                                jf.OfficialRating,
                                jf.CommunityRating?.let { "★ ${String.format("%.1f", it)}" }
                            ).joinToString(" • ")
                        },
                        posterUrl = JellyfinApi.imageUrl(JellyfinSession.serverUrl, jf, "Primary"),
                        backdropUrl = JellyfinApi.imageUrl(JellyfinSession.serverUrl, jf, "Backdrop", 1280),
                        jellyfinId = jf.Id,
                        isSeries = jf.Type == "Series",
                        ratingLevel = ratingToLevel(jf.OfficialRating)
                    )
                }

                heroItems = toContent(latest.take(5))
                showHero(0)
                handler.postDelayed(heroRotator, 8000)

                val shelfList = mutableListOf<Shelf>()
                if (resume.isNotEmpty())
                    shelfList.add(Shelf("Continue Watching", toContent(resume), Shelf.WIDE))
                shelfList.add(Shelf("Recently Added", toContent(latest), Shelf.POSTER))

                libraries.forEach { lib ->
                    val items = withContext(Dispatchers.IO) {
                        api.getItems(uid, lib.Id).Items
                    }
                    if (items.isNotEmpty())
                        shelfList.add(Shelf(lib.Name, toContent(items), Shelf.POSTER))
                }
                shelfList.add(Shelf("Your Apps", getInstalledApps(), Shelf.SQUARE))

                shelves.layoutManager =
                    LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
                shelves.adapter = ShelfAdapter(shelfList) { onItemFocused(it) }

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Jellyfin unreachable — showing offline content",
                    Toast.LENGTH_SHORT).show()
                setupFallback()
            }
        }
    }

    private fun setupFallback() {
        heroItems = listOf(
            ContentItem(1, "Ted Lasso", "Comedy • TV-MA"),
            ContentItem(2, "Severance", "Thriller • TV-MA"))
        showHero(0)
        shelves.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        shelves.adapter = ShelfAdapter(listOf(
            Shelf("Up Next", heroItems, Shelf.WIDE),
            Shelf("Your Apps", getInstalledApps(), Shelf.SQUARE)
        )) { }
    }

    private fun showHero(index: Int) {
        heroIndex = index
        val item = heroItems[index]

        heroImage.animate().alpha(0f).setDuration(300).withEndAction {
            Glide.with(this).load(item.backdropUrl ?: item.posterUrl)
                .centerCrop().into(heroImage)
            heroImage.animate().alpha(1f).setDuration(500).start()
        }.start()

        Glide.with(this).load(item.backdropUrl ?: item.posterUrl)
            .centerCrop().into(background)

        listOf(heroTitle, heroMeta).forEach { v ->
            v.alpha = 0f; v.translationY = 20f
            v.animate().alpha(1f).translationY(0f).setDuration(400)
                .setStartDelay(150).setInterpolator(DecelerateInterpolator()).start()
        }
        heroTitle.text = item.title
        heroMeta.text = item.subtitle
    }

    fun onItemFocused(item: ContentItem) {
        if (!item.isApp && item.backdropUrl != null) {
            handler.removeCallbacks(heroRotator)
            showHero(heroItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0))
            handler.postDelayed(heroRotator, 8000)
        }
    }

    private fun ratingToLevel(rating: String?): Int = when (rating?.uppercase()) {
        "G", "TV-Y", "TV-G" -> 0
        "PG", "TV-PG" -> 1
        "PG-13", "TV-14" -> 2
        else -> 4
    }

    private fun getInstalledApps(): List<ContentItem> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0).mapNotNull { info ->
            if (info.activityInfo.packageName == packageName) return@mapNotNull null
            ContentItem(
                id = info.hashCode().toLong(),
                title = info.loadLabel(pm).toString(),
                isApp = true,
                launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { moveTaskToBack(true) }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
    }
}