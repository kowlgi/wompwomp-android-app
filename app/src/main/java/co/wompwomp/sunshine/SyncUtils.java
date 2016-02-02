/*
 * Copyright 2013 Google Inc.
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

package co.wompwomp.sunshine;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.accounts.GenericAccountService;
import timber.log.Timber;

/**
 * Static helper methods for working with the sync framework.
 */
public class SyncUtils {
    private static final String CONTENT_AUTHORITY = FeedContract.CONTENT_AUTHORITY;
    private static final String PREF_SETUP_COMPLETE = "setup_complete";
    // Value below must match the account type specified in res/xml/syncadapter.xml
    public static final String ACCOUNT_TYPE = BuildConfig.ACCOUNT_TYPE;

    /**
     * Create an entry for this application in the system account list, if it isn't already there.
     *
     * @param context Context
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    public static void CreateSyncAccount(Context context) {
        boolean newAccount = false;
        boolean setupComplete = PreferenceManager
                .getDefaultSharedPreferences(context).getBoolean(PREF_SETUP_COMPLETE, false);

        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = GenericAccountService.GetAccount(ACCOUNT_TYPE);
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(account, null, null)) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
            newAccount = true;
        }
        // disable auto sync: it was enabled in apk v1.1.6 and we're disabling it starting v1.1.7
        ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, false);
    }

    /**
     * Helper method to trigger an immediate sync ("refresh").
     *
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     *
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public static void TriggerRefresh(WompWompConstants.SyncMethod syncMethod) {
        Bundle b = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        b.putString(WompWompConstants.SYNC_METHOD, syncMethod.name());

        ContentResolver.requestSync(
                GenericAccountService.GetAccount(ACCOUNT_TYPE), // Sync account
                FeedContract.CONTENT_AUTHORITY,                 // Content authority
                b);                                             // Extras
    }

    public static void TriggerSync(WompWompConstants.SyncMethod syncMethod) {
        Bundle b = new Bundle();
        b.putString(WompWompConstants.SYNC_METHOD, syncMethod.name());

        ContentResolver.requestSync(
                GenericAccountService.GetAccount(ACCOUNT_TYPE), // Sync account
                FeedContract.CONTENT_AUTHORITY,                 // Content authority
                b);                                             // Extras
    }
}
