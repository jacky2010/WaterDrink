package com.skydoves.waterdays.ui.activities.settings

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.skydoves.waterdays.R
import kotlinx.android.synthetic.main.activity_nfc.*
import java.io.IOException

/**
 * Created by skydoves on 2016-10-15.
 * Updated by skydoves on 2017-08-17.
 * Copyright (c) 2017 skydoves rights reserved.
 */

class NFCActivity : AppCompatActivity() {

    internal var mWriteMode = false
    private var mNfcAdapter: NfcAdapter? = null
    private var mNfcPendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)

        nfc_sticker.startRippleAnimation()

        nfc_btn_setnfcdata.setOnClickListener {
            if (CheckNFCEnabled()) {
                mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
                mNfcPendingIntent = PendingIntent.getActivity(this, 0, Intent(this, NFCActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
                enableTagWriteMode()
                nfc_sticker.setVisibility(View.VISIBLE)
            } else {
                val setnfc = Intent(Settings.ACTION_NFC_SETTINGS)
                startActivity(setnfc)
                Toast.makeText(this, getString(R.string.msg_require_nfc), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun CheckNFCEnabled(): Boolean {
        val nfcAdpt = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdpt != null) {
            if (!nfcAdpt.isEnabled)
                return false
        }
        return true
    }

    private fun enableTagWriteMode() {
        mWriteMode = true
        val tagDetected = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val mWriteTagFilters = arrayOf(tagDetected)
        mNfcAdapter!!.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null)
    }

    private fun disableTagWriteMode() {
        mWriteMode = false
    }

    override fun onNewIntent(intent: Intent) {
        if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val detectedTag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            val record = NdefRecord.createMime("waterdays_nfc/NFC", (findViewById(R.id.nfc_edt01) as TextView).text.toString().toByteArray())
            val message = NdefMessage(arrayOf(record))

            // detected tag
            if (writeTag(message, detectedTag)) {
                disableTagWriteMode()
                nfc_sticker.setVisibility(View.INVISIBLE)
                Toast.makeText(this, "NFC 태그에 데이터를 작성했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun writeTag(message: NdefMessage, tag: Tag): Boolean {
        val size = message.toByteArray().size
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    Toast.makeText(applicationContext, "태그에 데이터를 작성할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return false
                }
                if (ndef.maxSize < size) {
                    Toast.makeText(applicationContext, "태그 사이즈가 너무 작습니다.", Toast.LENGTH_SHORT).show()
                    return false
                }
                ndef.writeNdefMessage(message)
                return true
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    try {
                        format.connect()
                        format.format(message)
                        return true
                    } catch (e: IOException) {
                        return false
                    }

                } else {
                    return false
                }
            }
        } catch (e: Exception) {
            return false
        }

    }
}