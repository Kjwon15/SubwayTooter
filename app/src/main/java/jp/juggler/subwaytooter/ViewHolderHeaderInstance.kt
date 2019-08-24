package jp.juggler.subwaytooter

import android.content.Intent
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import jp.juggler.subwaytooter.action.Action_Account
import jp.juggler.subwaytooter.api.entity.TootInstance
import jp.juggler.subwaytooter.util.DecodeOptions
import jp.juggler.subwaytooter.view.MyLinkMovementMethod
import jp.juggler.subwaytooter.view.MyNetworkImageView
import jp.juggler.util.LogCategory
import jp.juggler.util.showToast
import org.conscrypt.OpenSSLX509Certificate
import java.util.*
import java.util.regex.Pattern

internal class ViewHolderHeaderInstance(
	arg_activity : ActMain,
	viewRoot : View
) : ViewHolderHeaderBase(arg_activity, viewRoot)
	, View.OnClickListener {
	
	companion object {
		private val log = LogCategory("ViewHolderHeaderInstance")
		
		private val reWhitespaceBeforeLineFeed = Pattern.compile("[ \t\r]+\n")
	}
	
	private val btnInstance : TextView
	private val tvVersion : TextView
	private val tvTitle : TextView
	private val btnEmail : TextView
	private val tvDescription : TextView
	private val tvUserCount : TextView
	private val tvTootCount : TextView
	private val tvDomainCount : TextView
	private val ivThumbnail : MyNetworkImageView
	private val btnContact : TextView
	private val tvLanguages : TextView
	private val tvHandshake : TextView
	
	private var instance : TootInstance? = null
	
	init {
		
		//
		//		CharSequence sv = HTMLDecoder.decodeHTML( activity, access_info, html, false, true, null );
		//
		//		TextView tvSearchDesc = (TextView) viewRoot.findViewById( R.id.tvSearchDesc );
		//		tvSearchDesc.setVisibility( View.VISIBLE );
		//		tvSearchDesc.setMovementMethod( MyLinkMovementMethod.getInstance() );
		//		tvSearchDesc.setText( sv );
		
		btnInstance = viewRoot.findViewById(R.id.btnInstance)
		tvVersion = viewRoot.findViewById(R.id.tvVersion)
		tvTitle = viewRoot.findViewById(R.id.tvTitle)
		btnEmail = viewRoot.findViewById(R.id.btnEmail)
		tvDescription = viewRoot.findViewById(R.id.tvDescription)
		tvUserCount = viewRoot.findViewById(R.id.tvUserCount)
		tvTootCount = viewRoot.findViewById(R.id.tvTootCount)
		tvDomainCount = viewRoot.findViewById(R.id.tvDomainCount)
		ivThumbnail = viewRoot.findViewById(R.id.ivThumbnail)
		btnContact = viewRoot.findViewById(R.id.btnContact)
		tvLanguages = viewRoot.findViewById(R.id.tvLanguages)
		tvHandshake = viewRoot.findViewById(R.id.tvHandshake)
		
		btnInstance.setOnClickListener(this)
		btnEmail.setOnClickListener(this)
		btnContact.setOnClickListener(this)
		ivThumbnail.setOnClickListener(this)
		
		
		viewRoot.findViewById<View>(R.id.btnAbout)?.setOnClickListener(this)
		viewRoot.findViewById<View>(R.id.btnAboutMore)?.setOnClickListener(this)
		viewRoot.findViewById<View>(R.id.btnExplore)?.setOnClickListener(this)
		
		tvDescription.movementMethod = MyLinkMovementMethod
	}
	
	override fun showColor() {
		//
	}
	
	override fun bindData(column : Column) {
		super.bindData(column)
		val instance = column.instance_information
		val handshake = column.handshake
		this.instance = instance
		
		if(instance == null) {
			btnInstance.text = "?"
			tvVersion.text = "?"
			tvTitle.text = "?"
			btnEmail.text = "?"
			btnEmail.isEnabled = false
			tvDescription.text = "?"
			ivThumbnail.setImageUrl(App1.pref, 0f, null)
			tvLanguages.text = "?"
			btnContact.text = "?"
			btnContact.isEnabled = false
		} else {
			val uri = instance.uri ?: ""
			btnInstance.text = uri
			btnInstance.isEnabled = uri.isNotEmpty()
			
			tvVersion.text = instance.version ?: ""
			tvTitle.text = instance.title ?: ""
			
			val email = instance.email ?: ""
			btnEmail.text = email
			btnEmail.isEnabled = email.isNotEmpty()
			
			val contact_acct =
				instance.contact_account?.let { who -> "@" + who.username + "@" + who.host } ?: ""
			btnContact.text = contact_acct
			btnContact.isEnabled = contact_acct.isNotEmpty()
			
			tvLanguages.text = instance.languages?.joinToString(", ") ?: ""
			
			val sb = DecodeOptions(activity, access_info, decodeEmoji = true)
				.decodeHTML("<p>" + (instance.description ?: "") + "</p>")
			
			// 行末の空白を除去
			val m = reWhitespaceBeforeLineFeed.matcher(sb)
			val matchList = LinkedList<Pair<Int, Int>>()
			while(m.find()) {
				// 逆順に並べる
				matchList.addFirst(Pair(m.start(), m.end()))
			}
			for(pair in matchList) {
				sb.delete(pair.first, pair.second - 1)
			}
			
			// 連続する改行をまとめる
			var previous_br_count = 0
			var i = 0
			while(i < sb.length) {
				val c = sb[i]
				if(c == '\n') {
					if(++ previous_br_count >= 3) {
						sb.delete(i, i + 1)
						continue
					}
				} else {
					previous_br_count = 0
				}
				++ i
			}
			
			tvDescription.text = sb
			
			val stats = instance.stats
			if(stats == null) {
				tvUserCount.setText(R.string.not_provided_mastodon_under_1_6)
				tvTootCount.setText(R.string.not_provided_mastodon_under_1_6)
				tvDomainCount.setText(R.string.not_provided_mastodon_under_1_6)
			} else {
				tvUserCount.text = stats.user_count.toString(10)
				tvTootCount.text = stats.status_count.toString(10)
				tvDomainCount.text = stats.domain_count.toString(10)
				
			}
			
			val thumbnail = instance.thumbnail
			if(thumbnail == null || thumbnail.isEmpty()) {
				ivThumbnail.setImageUrl(App1.pref, 0f, null)
			} else {
				ivThumbnail.setImageUrl(App1.pref, 0f, thumbnail, thumbnail)
			}
		}
		
		tvHandshake.text = if(handshake == null) {
			""
		} else {
			val sb = SpannableStringBuilder("${handshake.tlsVersion}, ${handshake.cipherSuite}")
			val certs = handshake.peerCertificates.joinToString("\n") { cert ->
				"\n============================\n" +
					if(cert is OpenSSLX509Certificate) {
						
						log.d(cert.toString())
						
						"""
						Certificate : ${cert.type}
						subject : ${cert.subjectDN}
						subjectAlternativeNames : ${
						cert.subjectAlternativeNames
							?.joinToString(", ") {
								try {
									it?.last()
								} catch(ignored : Throwable) {
									it
								}
									?.toString() ?: "null"
							}
						}
						issuer : ${cert.issuerX500Principal}
						end : ${cert.notAfter}
						""".trimIndent()
						
					} else {
						cert.javaClass.name + "\n" + cert.toString()
					}
			}
			if(certs.isNotEmpty()) {
				sb.append('\n')
				sb.append(certs)
			}
			sb
		}
	}
	
	override fun onClick(v : View) {
		when(v.id) {
			
			R.id.btnEmail -> instance?.email?.let { email ->
				try {
					if(email.contains("://")) {
						App1.openCustomTab(activity, email)
					} else {
						val intent = Intent(Intent.ACTION_SEND)
						intent.type = "text/plain"
						intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
						intent.putExtra(Intent.EXTRA_TEXT, email)
						activity.startActivity(intent)
					}
					
				} catch(ex : Throwable) {
					log.e(ex, "startActivity failed. mail=$email")
					showToast(activity, true, R.string.missing_mail_app)
				}
				
			}
			
			R.id.btnContact -> instance?.contact_account?.let { who ->
				Action_Account.timeline(
					activity
					, activity.nextPosition(column)
					, ColumnType.SEARCH
					, bAllowPseudo = false
					, args = arrayOf("@" + who.username + "@" + who.host, true)
				)
			}
			
			R.id.btnInstance -> App1.openBrowser(activity, "https://${column.instance_uri}/about")
			R.id.ivThumbnail -> App1.openBrowser(activity, instance?.thumbnail)
			R.id.btnAbout -> App1.openBrowser(activity, "https://${column.instance_uri}/about")
			R.id.btnAboutMore ->
				App1.openBrowser(activity, "https://${column.instance_uri}/about/more")
			R.id.btnExplore -> App1.openBrowser(activity, "https://${column.instance_uri}/explore")
		}
	}
	
	override fun onViewRecycled() {
	}
	
}
