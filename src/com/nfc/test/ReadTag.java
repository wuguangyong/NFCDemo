package com.nfc.test;

import java.io.IOException;
import java.nio.charset.Charset;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class ReadTag extends Activity {
	private NfcAdapter nfcAdapter;
	private TextView resultText;
	private PendingIntent pendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;
	private Button mJumpTagBtn;
	private boolean isFirst = true;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.read_tag);
		// 获取nfc适配器，判断设备是否支持NFC功能
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (nfcAdapter == null) {
			Toast.makeText(this, getResources().getString(R.string.no_nfc),
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		} else if (!nfcAdapter.isEnabled()) {
			Toast.makeText(this, getResources().getString(R.string.open_nfc),
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		System.out.println("============3333333============");
		// 显示结果Text
		resultText = (TextView) findViewById(R.id.resultText);
		// 写入标签按钮
		mJumpTagBtn = (Button) findViewById(R.id.jump);
		mJumpTagBtn.setOnClickListener(new WriteBtnOnClick());

		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		ndef.addCategory("*/*");
		mFilters = new IntentFilter[] { ndef };// 过滤器
		mTechLists = new String[][] {
				new String[] { MifareClassic.class.getName() },
				new String[] { NfcA.class.getName() } };// 允许扫描的标签类型
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		nfcAdapter.enableForegroundDispatch(this, pendingIntent, mFilters,
				mTechLists);
		if (isFirst) {
			if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent()
					.getAction())) {
				String result = processIntent(getIntent());
				resultText.setText(result);
			}
			isFirst = false;
		}

	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
			String result = processIntent(intent);
			resultText.setText(result);
		}
	}

	/**
	 * 获取tab标签中的内容
	 * 
	 * @param intent
	 * @return
	 */
	@SuppressLint("NewApi")
	private String processIntent(Intent intent) {
		// 取出封装在intent中的TAG
		Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		String metaInfo = "";
		if (tagFromIntent != null) {
			try {
				read(tagFromIntent);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			metaInfo += "卡片ID：" + bytesToHexString(tagFromIntent.getId(), true)+"\n";
			// Tech List
			String[] techList = tagFromIntent.getTechList();
			String CardType = "";
			for (int i = 0; techList != null && i < techList.length; i++) {
				if (techList[i].equals(NfcA.class.getName())) {
					// 读取TAG
					NfcA mfc = NfcA.get(tagFromIntent);
					try {
						System.out.println("======="+mfc.toString());
						metaInfo += mfc.toString();
						if ("".equals(CardType))
							CardType = "MifareClassic卡片类型 \n 不支持NDEF消息 \n";
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (techList[i].equals(MifareUltralight.class.getName())) {
					MifareUltralight mifareUlTag = MifareUltralight
							.get(tagFromIntent);
					String lightType = "";
					// Type Info
					switch (mifareUlTag.getType()) {
					case MifareUltralight.TYPE_ULTRALIGHT:
						lightType = "Ultralight";
						break;
					case MifareUltralight.TYPE_ULTRALIGHT_C:
						lightType = "Ultralight C";
						break;
					}
					CardType = lightType + "卡片类型\n";
					metaInfo += lightType +"卡片类型--\n";

					Ndef ndef = Ndef.get(tagFromIntent);
					CardType += "最大数据尺寸:" + ndef.getMaxSize() + "\n";

				}
			}
//			metaInfo += CardType;

		}

		Parcelable[] rawmsgs = intent
				.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		if (rawmsgs == null)
			return metaInfo;
		NdefMessage msg = (NdefMessage) rawmsgs[0];
		NdefRecord[] records = msg.getRecords();
		String resultStr = new String(records[0].getPayload());
		return resultStr;
	}

	// 读取方法
	private String read(Tag tag) throws Exception, FormatException {
		if (tag != null) {
			// 解析Tag获取到NDEF实例
			Ndef ndef = Ndef.get(tag);
			// 打开连接
			ndef.connect();
			// 获取NDEF消息
			NdefMessage message = ndef.getNdefMessage();
			// 将消息转换成字节数组
			byte[] data = message.toByteArray();
			// 将字节数组转换成字符串
			String str = new String(data, Charset.forName("UTF-8"));
			System.out.println("======read==" + str);
			// 关闭连接
			ndef.close();
			return str;
		} else {
			Toast.makeText(this, "设备与nfc卡连接断开，请重新连接...", Toast.LENGTH_SHORT)
					.show();
		}
		return null;
	}

	private String bytesToHexString(byte[] src, boolean isPrefix) {
		StringBuilder stringBuilder = new StringBuilder();
		if (isPrefix == true) {
			stringBuilder.append("0x");
		}
		if (src == null || src.length <= 0) {
			return null;
		}
		char[] buffer = new char[2];
		for (int i = 0; i < src.length; i++) {
			buffer[0] = Character.toUpperCase(Character.forDigit(
					(src[i] >>> 4) & 0x0F, 16));
			buffer[1] = Character.toUpperCase(Character.forDigit(src[i] & 0x0F,
					16));
			System.out.println(buffer);
			stringBuilder.append(buffer);
		}
		return stringBuilder.toString();
	}

	/**
	 * 閹稿鎸抽悙鐟板毊娴滃娆�
	 * 
	 *
	 * 
	 */
	class WriteBtnOnClick implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.jump:
				Intent intent = new Intent(ReadTag.this, WriteTag.class);
				startActivity(intent);
			default:
				break;
			}
		}

	}

}
