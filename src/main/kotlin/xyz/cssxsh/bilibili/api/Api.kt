package xyz.cssxsh.bilibili.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import xyz.cssxsh.bilibili.*
import xyz.cssxsh.bilibili.data.*
import java.io.*
import java.security.*

// Base
const val INDEX_PAGE = "https://www.bilibili.com"
const val SPACE = "https://space.bilibili.com"

// Login
const val QRCODE_GENERATE = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate"
const val QRCODE_POLL = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll"

// Nav

const val WBI = "https://api.bilibili.com/x/web-interface/nav"

// User
const val SPACE_INFO = "https://api.bilibili.com/x/space/wbi/acc/info"

// Video
const val VIDEO_USER = "https://api.bilibili.com/x/space/wbi/arc/search"
const val VIDEO_INFO = "https://api.bilibili.com/x/web-interface/view"

// Dynamic
const val DYNAMIC_HISTORY = "https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/space_history"
const val DYNAMIC_INFO = "https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/get_dynamic_detail"

// Live
const val ROOM_INIT = "https://api.live.bilibili.com/room/v1/Room/room_init"
const val ROOM_INFO_OLD = "https://api.live.bilibili.com/room/v1/Room/getRoomInfoOld"
const val ROOM_INFO = "https://api.live.bilibili.com/xlive/web-room/v1/index/getInfoByRoom"
const val ROOM_OFF_LIVE = "https://api.live.bilibili.com/xlive/web-room/v1/index/getOffLiveList"
const val ROOM_ROUND_PLAY = "https://api.live.bilibili.com/live/getRoundPlayVideo"
const val ROOM_MULTIPLE = "https://api.live.bilibili.com/user/v3/User/getMultiple"

// Article
const val MORE = "https://api.bilibili.com/x/article/more?aid=15155534&platform=h5"
const val ARTICLES = "https://api.bilibili.com/x/article/list/web/articles"
const val ARTICLE_LIST_INFO = "https://api.bilibili.com/x/article/listinfo"
const val ARTICLE_VIEW_INFO = "https://api.bilibili.com/x/article/viewinfo"

// Music
const val MUSIC_INFO = "https://www.bilibili.com/audio/music-service-c/web/song/info"

// Media
const val SEASON_MEDIA_INFO = "https://api.bilibili.com/pgc/review/user"
const val SEASON_INFO = "https://api.bilibili.com/pgc/view/web/season"
const val SEASON_RECOMMEND = "https://api.bilibili.com/pgc/season/web/related/recommend?season_id=38234"
const val SEASON_SECTION = "https://api.bilibili.com/pgc/web/season/section"
const val SEASON_EPISODE = "https://api.bilibili.com/pgc/view/web/section/order?ep_id=395214"
const val BANGUMI_TIMELINE = "https://bangumi.bilibili.com/api/timeline_v2_global"
const val BANGUMI_TIMELINE_CN = "https://bangumi.bilibili.com/api/timeline_v2_cn"

// Tag
const val TAG_INFO = "https://api.bilibili.com/x/tag/info?tag_name=MEGALOBOX"
const val TAG_DETAIL = "https://api.bilibili.com/x/tag/detail?pn=0&ps=1&tag_id=18828932"
const val TAG_VIDEO = "https://api.bilibili.com/x/web-interface/tag/top?pn=1&ps=10&tid=18828932"

// Search
const val SEARCH_ALL = "https://api.bilibili.com/x/web-interface/search/all/v2"
const val SEARCH_TYPE = "https://api.bilibili.com/x/web-interface/search/type"

// Suit
const val SUIT_ITEMS = "https://api.bilibili.com/x/garb/mall/item/suit/v2"

// Emote
const val EMOTE_PANEL = "https://api.bilibili.com/x/emote/setting/panel"
const val EMOTE_PACKAGE = "https://api.bilibili.com/x/emote/package"

data class BiliApiException(
    val data: TempData,
    val url: Url
) : IllegalStateException() {
    override val message = "${data.message} in $url"
}

const val EXCEPTION_JSON_CACHE = "xyz.cssxsh.bilibili.api.exception"

const val JSON_IGNORE = "xyz.cssxsh.bilibili.api.ignore"

internal suspend inline fun <reified T> BiliClient.json(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit
): T = useHttpClient { client, mutex ->
    val url = Url(urlString)
    mutex.wait(url.encodedPath)
    client.prepareGet(url) {
        block.invoke(this)
        if ("wbi" in url.encodedPath) {
            encodeWbi(builder = this.url.parameters)
        }
    }.execute { response ->
        val temp = response.body<TempData>()
        if (temp.code != 0) throw BiliApiException(temp, response.request.url)
        val element = temp.data ?: temp.result ?: throw BiliApiException(temp, response.request.url)
        try {
            BiliClient.Json.decodeFromJsonElement(element)
        } catch (cause: SerializationException) {
            val path = System.getProperty(EXCEPTION_JSON_CACHE)
            supervisorScope {
                if (path != null) launch {
                    val folder = File(path)
                    folder.mkdirs()
                    folder.resolve("exception.${System.currentTimeMillis()}.json")
                        .writeText(BiliClient.Json.encodeToString(element))
                }
            }
            throw cause
        }
    }
}

private fun getMixinKey(ae: String): String {
    val oe = arrayOf(46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52)
    return buildString {
        for (i in oe) {
            append(ae[i])
            if (length >= 32) break
        }
    }
}

internal fun BiliClient.encodeWbi(builder: ParametersBuilder) {
    val salt = tokens.getOrPut("salt") {
        val data = runBlocking {
            val body = useHttpClient { http, _ ->
                http.get(WBI).body<JsonObject>()
            }
            body.getValue("data") as JsonObject
        }
        val images = BiliClient.Json.decodeFromJsonElement<WbiImages>(data.getValue("wbi_img"))
        val a = images.imgUrl.substringAfter("wbi/").substringBefore(".")
        val b = images.subUrl.substringAfter("wbi/").substringBefore(".")

        getMixinKey(a + b)
    }

    builder.append("wts", "1684763359")

    val digest = MessageDigest.getInstance("MD5")
    val parameters = builder.build().entries()
        .flatMap { e -> e.value.map { e.key to it } }
        .sortedBy { it.first }.formUrlEncode()

    val md5 = digest.digest((parameters + salt).encodeToByteArray())

    builder.append("w_rid", md5.joinToString("") { "%02x".format(it) })
}