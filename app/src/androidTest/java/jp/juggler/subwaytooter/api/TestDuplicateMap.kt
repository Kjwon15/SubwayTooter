package jp.juggler.subwaytooter.api

import androidx.test.runner.AndroidJUnit4
import android.test.mock.MockContext
import jp.juggler.subwaytooter.api.entity.*
import jp.juggler.subwaytooter.table.SavedAccount
import org.json.JSONObject

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

@RunWith(AndroidJUnit4::class)
class TestDuplicateMap {
	
	private val parser = TootParser(
		MockContext(),
		SavedAccount(
			db_id = 1,
			acct = "user1@host1",
			hostArg = null
		)
	)
	
	private val generatedItems = ArrayList<TimelineItem>()
	
	private fun genStatus(
		parser : TootParser,
		accountJson : JSONObject,
		statusId : String,
		uri : String?,
		url : String?
	):TootStatus{
		val itemJson = JSONObject()
		
		itemJson.apply {
			put("account", accountJson)
			put("id", statusId)
			if(uri != null) put("uri", uri)
			if(url != null) put("url", url)
		}
		
		return TootStatus(parser,itemJson)
	}
	
	private fun checkStatus(
		map : DuplicateMap,
		parser : TootParser,
		accountJson : JSONObject,
		statusId : String,
		uri : String?,
		url : String?
	) {
		val item = genStatus(parser,accountJson,statusId,uri,url)
		assertNotNull(item)
		generatedItems.add(item)
		assertEquals(false, map.isDuplicate(item))
		assertEquals(true, map.isDuplicate(item))
	}
	
	private fun testDuplicateStatus() {
		
		val account1Json = JSONObject()
		account1Json.apply {
			put("username", "user1")
			put("acct", "user1")
			put("id", 1L)
			put("url", "http://${parser.accessHost}/@user1")
		}
		
		val account1 = TootAccount(parser,account1Json)
		assertNotNull(account1)
		
		val map = DuplicateMap()
		
		// 普通のステータス
		checkStatus(
			map,
			parser,
			account1Json,
			"1",
			"http://${parser.accessHost}/@${account1.username}/1",
			"http://${parser.accessHost}/@${account1.username}/1"
		)
		// 別のステータス
		checkStatus(
			map,
			parser,
			account1Json,
			"2",
			"http://${parser.accessHost}/@${account1.username}/2",
			"http://${parser.accessHost}/@${account1.username}/2"
		)
		// 今度はuriがない
		checkStatus(
			map,
			parser,
			account1Json,
			"3",
			null, // "http://${parser.accessHost}/@${account1.username}/3",
			"http://${parser.accessHost}/@${account1.username}/3"
		)
		// 今度はuriとURLがない
		checkStatus(
			map,
			parser,
			account1Json,
			"4",
			null, // "http://${parser.accessHost}/@${account1.username}/4",
			null //"http://${parser.accessHost}/@${account1.username}/4"
		)
		// 今度はIDがおかしい
		checkStatus(
			map,
			parser,
			account1Json,
			"",
			null, // "http://${parser.accessHost}/@${account1.username}/4",
			null //"http://${parser.accessHost}/@${account1.username}/4"
		)
		
	}
	
	
	private fun checkNotification(
		map : DuplicateMap,
		parser : TootParser,
		id : Long
	) {
		val itemJson = JSONObject()
		
		itemJson.apply {
			put("type", TootNotification.TYPE_MENTION)
			put("id", id)
		}
		
		val item = TootNotification( parser,itemJson )
		assertNotNull(item)
		generatedItems.add(item)
		assertEquals(false, map.isDuplicate(item))
		assertEquals(true, map.isDuplicate(item))
	}
	
	private fun testDuplicateNotification() {
		val map = DuplicateMap()
		checkNotification(map,parser,0L)
		checkNotification(map,parser,1L)
		checkNotification(map,parser,2L)
		checkNotification(map,parser,3L)
	}
	
	private fun checkReport(
		map : DuplicateMap,
		id : Long
	) {
		val item = TootReport(JSONObject().apply{
			put("id",id)
			put("action_taken","eat")
		})
		
		assertNotNull(item)
		generatedItems.add(item)
		assertEquals(false, map.isDuplicate(item))
		assertEquals(true, map.isDuplicate(item))
	}
	
	
	private fun testDuplicateReport() {
		val map = DuplicateMap()
		checkReport(map,0L)
		checkReport(map,1L)
		checkReport(map,2L)
		checkReport(map,3L)
	}
	
	private fun checkAccount(
		map : DuplicateMap,
		parser : TootParser,
		id : Long
	) {

		val itemJson = JSONObject()
		itemJson.apply {
			put("username", "user$id")
			put("acct", "user$id")
			put("id", id)
			put("url", "http://${parser.accessHost}/@user$id")
		}
		
		val item = TootAccountRef.notNull(parser,TootAccount(parser,itemJson))
		assertNotNull(item)
		generatedItems.add(item)
		assertEquals(false, map.isDuplicate(item))
		assertEquals(true, map.isDuplicate(item)) // 二回目はtrueになる
	}
	
	private fun testDuplicateAccount() {
		val map = DuplicateMap()
		checkAccount(map,parser,0L)
		checkAccount(map,parser,1L)
		checkAccount(map,parser,2L)
		checkAccount(map,parser,3L)
	}
	
	@Test fun testFilterList(){
		generatedItems.clear()
		testDuplicateStatus()
		testDuplicateNotification()
		testDuplicateReport()
		testDuplicateAccount()
		
		val map = DuplicateMap()

		val dst = map.filterDuplicate( generatedItems)
		assertEquals( generatedItems.size,dst.size)

		val dst2 = map.filterDuplicate( generatedItems)
		assertEquals( 0,dst2.size)
	}
}
