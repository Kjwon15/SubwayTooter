package jp.juggler.subwaytooter.action

import jp.juggler.subwaytooter.*
import jp.juggler.subwaytooter.dialog.ActionsDialog
import jp.juggler.subwaytooter.table.AcctColor
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.util.encodePercent
import java.util.*

object Action_HashTag {
	
	// ハッシュタグへの操作を選択する
	fun dialog(
		activity : ActMain,
		pos : Int,
		url : String,
		host : String,
		tag_without_sharp : String,
		tag_list : ArrayList<String>?,
		whoAcct: String?
	) {
		val tag_with_sharp = "#$tag_without_sharp"
		
		val d = ActionsDialog()
			.addAction(activity.getString(R.string.open_hashtag_column)) {
				timelineOtherInstance(
					activity,
					pos,
					url,
					host,
					tag_without_sharp
				)
			}
		
		// https://mastodon.juggler.jp/@tateisu/101865456016473337
		// 一時的に使えなくする
		if( whoAcct != null ){
			val(username,instance)=whoAcct.split('@')
			d.addAction(AcctColor.getStringWithNickname(activity, R.string.open_hashtag_from_account ,whoAcct)) {
				timelineOtherInstance(
					activity,
					pos,
					"https://${instance}/@${username}/tagged/${ tag_without_sharp.encodePercent()}",
					host,
					tag_without_sharp,
					whoAcct
				)
			}
		}
		
		
		d.addAction(activity.getString(R.string.open_in_browser)) {
			App1.openCustomTab(
				activity,
				url
			)
		}
		.addAction(
			activity.getString(
				R.string.quote_hashtag_of,
				tag_with_sharp
			)
		) { Action_Account.openPost(activity, "$tag_with_sharp ") }
		
		
		if(tag_list != null && tag_list.size > 1) {
			val sb = StringBuilder()
			for(s in tag_list) {
				if(sb.isNotEmpty()) sb.append(' ')
				sb.append(s)
			}
			val tag_all = sb.toString()
			d.addAction(
				activity.getString(
					R.string.quote_all_hashtag_of,
					tag_all
				)
			) { Action_Account.openPost(activity, "$tag_all ") }
		}
		
		d.show(activity, tag_with_sharp)
	}
	
	// 検索カラムからハッシュタグを選んだ場合、カラムのアカウントでハッシュタグを開く
	fun timeline(
		activity : ActMain,
		pos : Int,
		access_info : SavedAccount,
		tag_without_sharp : String,
		whoAcct : String? = null
	) {
		if( whoAcct == null) {
			activity.addColumn(pos, access_info, ColumnType.HASHTAG, tag_without_sharp)
		}else {
			activity.addColumn(pos, access_info, ColumnType.HASHTAG_FROM_ACCT, tag_without_sharp,whoAcct)
		}
	}
	
	// アカウントを選んでハッシュタグカラムを開く
	fun timelineOtherInstance(
		activity : ActMain,
		pos : Int,
		url : String,
		host : String,
		tag_without_sharp : String,
		whoAcct :String? = null
	) {
		
		val dialog = ActionsDialog()
		
		// 各アカウント
		val account_list = SavedAccount.loadAccountList(activity)
		
		// ソートする
		SavedAccount.sort(account_list)
		
		// 分類する
		val list_original = ArrayList<SavedAccount>()
		val list_original_pseudo = ArrayList<SavedAccount>()
		val list_other = ArrayList<SavedAccount>()
		for(a in account_list) {
			if( whoAcct == null){
				if(! host.equals(a.host, ignoreCase = true)) {
					list_other.add(a)
				} else if(a.isPseudo) {
					list_original_pseudo.add(a)
				} else {
					list_original.add(a)
				}
			}else{
				if(a.isPseudo) {
					// acctからidを取得できない
				}else if(a.isMisskey) {
					// ミスキーのアカウント別タグTLは未対応
				}else if(! host.equals(a.host, ignoreCase = true)) {
					list_other.add(a)
				} else {
					list_original.add(a)
				}
			}
		}
		
		// ブラウザで表示する
		dialog.addAction(activity.getString(R.string.open_web_on_host, host)) {
			App1.openCustomTab(activity,url)
		}
		
		// 同タンスのアカウントがない場合は疑似アカウントを作成して開く
		// ただし疑似アカウントではアカウントの同期ができないため、特定ユーザのタグTLは読めない)
		if( whoAcct == null && list_original.isEmpty() && list_original_pseudo.isEmpty()) {
			dialog.addAction(activity.getString(R.string.open_in_pseudo_account, "?@$host")) {
				addPseudoAccount(activity, host) { sa ->
					timeline(activity, pos, sa, tag_without_sharp)
				}
			}
		}
		
		// 分類した順に選択肢を追加する
		for(a in list_original) {
			dialog.addAction(
				AcctColor.getStringWithNickname(
					activity,
					R.string.open_in_account,
					a.acct
				)
			)
			{ timeline(activity, pos, a, tag_without_sharp,whoAcct) }
		}
		for(a in list_original_pseudo) {
			dialog.addAction(
				AcctColor.getStringWithNickname(
					activity,
					R.string.open_in_account,
					a.acct
				)
			)
			{ timeline(activity, pos, a, tag_without_sharp,whoAcct) }
		}
		for(a in list_other) {
			dialog.addAction(
				AcctColor.getStringWithNickname(
					activity,
					R.string.open_in_account,
					a.acct
				)
			)
			{ timeline(activity, pos, a, tag_without_sharp,whoAcct) }
		}
		
		dialog.show(activity, "#$tag_without_sharp")
	}
	
}
