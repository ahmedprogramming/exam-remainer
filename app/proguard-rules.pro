# Room, WorkManager and Compose ship their own consumer rules.
# Keep broadcast receivers referenced from the manifest (AGP keeps these automatically,
# listed here for clarity).
-keep class com.studentreminder.app.reminders.** { *; }
