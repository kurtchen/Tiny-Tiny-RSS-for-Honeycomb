package org.fox.ttrss;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.security.MessageDigest;

import android.app.IntentService;
import android.widget.Toast;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRouteParams;

import org.fox.ttrss.types.Article;
import org.fox.ttrss.types.ArticleList;

public class TempOfflineDownloadService extends IntentService {
	private static final String TAG = TempOfflineDownloadService.class.getSimpleName();

	public TempOfflineDownloadService() {
		super(TAG);
		setIntentRedelivery(true);

		createDirs();
	}

	private String createDirs() {
		File appFilesDir = new File(Environment.getExternalStorageDirectory(), "ttrss/tmp_offline");
		if (!appFilesDir.exists()) {
			Log.d(TAG, "creating dir " + appFilesDir.getAbsolutePath());
			if (!appFilesDir.mkdirs()) {
				Log.e(TAG, "failed to mkdir " + appFilesDir.getAbsolutePath());
				return null;
			}
		}

		return appFilesDir.getAbsolutePath();
	}

	protected void onHandleIntent(Intent intent) {
		if (intent == null) {
			Log.e(TAG, "onHandleIntent() - intent is null");
			return;
		}

		ArticleList arList = (ArticleList) intent.getParcelableExtra("article_list");
		if (arList == null) {
			arList = new ArticleList();
		}
		Article ar = intent.getParcelableExtra("article");
		if (ar != null) {
			arList.add(ar);
		}

		if (arList.size() <= 0) {
			Log.w(TAG, "onHandleIntent() - no article found in intent");
			return;
		}

		for (Article article : arList) {
			saveArticle(article);
		}
	}

	private void saveArticle(Article ar) {

		if (ar == null || TextUtils.isEmpty(ar.link)) {
			Log.e(TAG, "saveArticle() - invalid article, skip");
			return;
		}

		showToast(getResources().getString(R.string.tmp_offline_downloading, ar.title));

		boolean downloadSuccess = true;

		// get the full article first
		String fullArticleLink = ar.getFullArticleLink();
		if (!TextUtils.isEmpty(fullArticleLink)) {
			AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Android");
			HttpGet httpGet = new HttpGet(fullArticleLink);
			HttpResponse resp = null;
			StatusLine statusLine = null;
			HttpEntity entity = null;
			BufferedReader br = null;
			try {
				resp = httpClient.execute(httpGet);
				statusLine = resp.getStatusLine();
				if (statusLine != null && statusLine.getStatusCode() == 200) {
					entity = resp.getEntity();
					if (entity != null) {
						br = new BufferedReader(new InputStreamReader(entity.getContent()));
						StringBuilder sb = new StringBuilder();
						String line = null;
						while ((line = br.readLine()) != null) {
							sb.append(line);
						}
						ar.content = sb.toString();
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "failed to fetch full article", e);
				downloadSuccess = false;
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (Exception e1) {
						Log.d(TAG, "failed to close input stream", e1);
					}
				}
				if (entity != null) {
					try {
						entity.consumeContent();
					} catch (Exception e1) {
						Log.d(TAG, "failed to consume entity", e1);
					}
				}
				httpClient.close();
			}
		}

		// save to disk
		String dir = createDirs();
		if (dir != null) {
			FileWriter fw = null;
			// mark as read
			ar.unread = false;
			try {
				Gson gson = new Gson();
				MessageDigest md = MessageDigest.getInstance("SHA-1");
				byte[] fileNameBytes = md.digest(ar.link.getBytes());
				String fileName = toHex(fileNameBytes);
				fw = new FileWriter(dir + "/" + fileName);
				gson.toJson(ar, Article.class, fw);
			} catch (Exception e) {
				Log.e(TAG, "failed to write disk", e);
				downloadSuccess = false;
			} finally {
				if (fw != null) {
					try {
						fw.close();
					} catch(Exception e1) {
						Log.e(TAG, "failed to close file writer", e1);
					}
				}
			}
		}

		if (downloadSuccess)
			showToast(getResources().getString(R.string.tmp_offline_download_success, ar.title));
		else
			showToast(getResources().getString(R.string.tmp_offline_download_fail, ar.title));
	}

	public static String toHex(byte[] ary) {
		final String hex = "0123456789ABCDEF";
		String ret = "";
		for (int i = 0; i < ary.length; i++) {
			ret += hex.charAt((ary[i] >> 4) & 0xf);
			ret += hex.charAt(ary[i] & 0xf);
		}
		return ret;
	}

	private Handler mMainThreadHandler = new Handler();

	private void showToast(final String text) {
		mMainThreadHandler.post(new Runnable() {
			public void run() {
				Toast.makeText(TempOfflineDownloadService.this.getApplicationContext(), text, Toast.LENGTH_LONG).show();
			}
		});
	}
}
