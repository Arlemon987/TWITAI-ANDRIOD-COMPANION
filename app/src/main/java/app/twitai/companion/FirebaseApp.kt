package app.twitai.companion

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseBootstrap {
    fun init(context: Context) {
        if (FirebaseApp.getApps(context).isNotEmpty()) return

        val options = FirebaseOptions.Builder()
            .setApiKey(Config.FIREBASE_API_KEY)
            .setApplicationId(Config.FIREBASE_APP_ID)
            .setProjectId(Config.FIREBASE_PROJECT_ID)
            .build()

        FirebaseApp.initializeApp(context, options)
    }
}
