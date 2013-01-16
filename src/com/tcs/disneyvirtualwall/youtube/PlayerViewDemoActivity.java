/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tcs.disneyvirtualwall.youtube;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.android.youtube.player.YouTubePlayer.PlayerStyle;
import com.tcs.disneyvirtualwall.R;
import com.tcs.disneyvirtualwall.R.id;
import com.tcs.disneyvirtualwall.R.layout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

/**
 * A simple YouTube Android API demo application which shows how to create a simple application that
 * displays a YouTube Video in a {@link YouTubePlayerView}.
 * <p>
 * Note, to use a {@link YouTubePlayerView}, your activity must extend {@link YouTubeBaseActivity}.
 */
public class PlayerViewDemoActivity extends YouTubeFailureRecoveryActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playerview_demo);

		YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
		youTubeView.initialize(DeveloperKey.DEVELOPER_KEY, this);
	}

	@Override
	public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
			boolean wasRestored) {
		player.setPlayerStyle(PlayerStyle.CHROMELESS);
		if (!wasRestored) {
			Intent intent = getIntent();
			String video_uri = intent.getStringExtra("video_uri");
			player.cueVideo(video_uri);
		}
		
		final YouTubePlayer fplayer = player;
		player.setPlayerStateChangeListener(new PlayerStateChangeListener(){

			@Override
			public void onAdStarted() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onError(ErrorReason arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onLoaded(String arg0) {
				// TODO Auto-generated method stub
				Log.v("DisneyVirtualWall","onLoaded");
				fplayer.play();
			}

			@Override
			public void onLoading() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onVideoEnded() {
				// TODO Auto-generated method stub
				PlayerViewDemoActivity.this.finish();
			}

			@Override
			public void onVideoStarted() {
				// TODO Auto-generated method stub
				
			}
			
		});
	}

	@Override
	protected YouTubePlayer.Provider getYouTubePlayerProvider() {
		return (YouTubePlayerView) findViewById(R.id.youtube_view);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	Log.w("[onKeyDown]", "keyCode: "+Integer.toString(keyCode));
    	if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0){
    		return false;
    	}
    	return true;
    }
}
