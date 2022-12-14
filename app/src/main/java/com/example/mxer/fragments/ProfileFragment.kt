package com.example.mxer.fragments
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mxer.Community
import com.example.mxer.R
import com.parse.*
import java.io.File

class ProfileFragment : Fragment() {
    var allCommunities: ArrayList<Community> = ArrayList()
    var userCommunities: ArrayList<Community> = ArrayList()
    var userEvents: ArrayList<Community> = ArrayList()

    private val CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034
    private val photoFileName = "photo.jpg"
    private var photoFile: File? = null
    private lateinit var ivPfp: ImageView
    private lateinit var etBio: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getOtherCommunities()
        getUserCommunities()
        getUserEvents()
        val tvUserName = view.findViewById<TextView>(R.id.username)
        val btnCreateEvent = view.findViewById<Button>(R.id.btn_createevent)
        val btnDeleteEvent = view.findViewById<Button>(R.id.btn_deleteevent)
        val tvBio = view.findViewById<TextView>(R.id.bio)
        val ivPfp = view.findViewById<ImageView>(R.id.profilePicture)
        val dialog = AlertDialog.Builder(requireContext()).create()
        etBio = EditText(requireContext())
        dialog.setTitle(" Edit Bio ")
        dialog.setView(etBio)

        val user = ParseUser.getCurrentUser()
        tvUserName.text = " " + user.username
        tvBio.text = user.getString("description")
        Glide.with(requireContext()).load(user.getParseFile("profile_picture")?.url).into(ivPfp)

        ivPfp.setOnClickListener {
            onLaunchCamera()
        }

        btnCreateEvent.setOnClickListener {
            createEvent()
        }

        btnDeleteEvent.setOnClickListener {
            deleteEvent()
        }

        tvBio.setOnClickListener {
            etBio.setText(tvBio.text)
            dialog.show()
//            Toast.makeText(requireContext(), "Mixing up bio", Toast.LENGTH_LONG).show()
        }

        dialog.setButton(DialogInterface.BUTTON_POSITIVE,"Save Bio") { dialog, id ->
            setBio(etBio)
            tvBio.text = user.getString("description")
        }


    }


    private fun onLaunchCamera() {
        // create Intent to take a picture and return control to the calling application
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Create a File reference for future access
        photoFile = getPhotoFileUri(photoFileName)

        // wrap File object into a content provider
        // required for API >= 24
        // See https://guides.codepath.com/android/Sharing-Content-with-Intents#sharing-files-with-api-24-or-higher
        if (photoFile != null) {
            val fileProvider: Uri =
                FileProvider.getUriForFile(
                    requireContext(),
                    "com.codepath.mxer.fileprovider",
                    photoFile!!
                )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

            // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
            // So as long as the result is not null, it's safe to use the intent.

            // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
            // So as long as the result is not null, it's safe to use the intent.
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                // Start the image capture intent to take photo
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
            }
        }
    }

    private fun getPhotoFileUri(fileName: String): File {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        val mediaStorageDir =
            File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG)

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "failed to create directory")
        }

        // Return the file target for the photo based on filename
        return File(mediaStorageDir.path + File.separator + fileName)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                // by this point we have the camera photo on disk
                setPfp(photoFile!!)
            } else { // Result was a failure
                Toast.makeText(requireContext(), "Error taking picture", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setBio(etBio: EditText) {
        val user = ParseUser.getCurrentUser()
        user.put("description", (etBio.text).toString())
        user.saveInBackground { e ->
            if (e == null) {
                Log.i(TAG, "Successfully saved bio")
                Toast.makeText(requireContext(), "Successfully updated bio!", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, e.printStackTrace().toString())
                Toast.makeText(requireContext(), "Unable to update bio.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setPfp(image: File) {
        val user = ParseUser.getCurrentUser()
        val pFile = ParseFile(image)
        user.put("profile_picture", pFile)
        user.saveInBackground { e ->
            if (e == null) {
                Log.i(TAG, "Successfully saved profile picture")
                Toast.makeText(
                    requireContext(),
                    "Successfully updated profile picture!",
                    Toast.LENGTH_SHORT
                ).show()
                val takenImage = BitmapFactory.decodeFile(photoFile!!.absolutePath)
                // RESIZE BITMAP, see section below
                // Load the taken image  into a preview
                ivPfp = requireView().findViewById(R.id.profilePicture)
                ivPfp.setImageBitmap(takenImage)
            } else {
                Log.e(TAG, e.printStackTrace().toString())
                Toast.makeText(
                    requireContext(),
                    "Unable to update profile picture.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun deleteEvent() {
        if (userEvents.isEmpty()) {
            Toast.makeText(requireContext(), "No Mxes to delete", Toast.LENGTH_SHORT).show()
        } else {
            val event = userEvents[0]
            event.setIsEvent(2)
            event.saveInBackground{ e ->
                Log.i(TAG, "Deleted")
                if (e == null) {
                    Toast.makeText(requireContext(), "Mxe deleted", Toast.LENGTH_SHORT).show()
                    userEvents.remove(event)
                } else {
                    Toast.makeText(requireContext(), "Unable to delete mxe", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getOtherCommunities() {
        val query: ParseQuery<Community> = ParseQuery.getQuery(Community::class.java)
        query.whereEqualTo("isEvent", 0)
        query.include(Community.KEY_OWNER)
        query.whereNotEqualTo(Community.KEY_OWNER, ParseUser.getCurrentUser())
        query.findInBackground { communities, e ->
            if (e != null) {
                // Something went wrong
                Log.e(TAG, "Error fetching communities")
            } else {
                if (communities != null) {
                    allCommunities.addAll(communities)
                    Log.i(TAG, "Communities: $allCommunities")
                }
            }
        }
    }

    private fun getUserCommunities() {
        val query: ParseQuery<Community> = ParseQuery.getQuery(Community::class.java)
        val tvPrimeCommunity = view?.findViewById<TextView>(R.id.communityName)
        query.whereEqualTo("isEvent", 0)
        query.include(Community.KEY_OWNER)
        query.whereEqualTo(Community.KEY_OWNER, ParseUser.getCurrentUser())
        query.findInBackground { communities, e ->
            if (e != null) {
                // Something went wrong
                Log.e(TAG, "Error fetching communities")
            } else {
                if (communities != null) {
                    userCommunities.addAll(communities)
                    Log.i(TAG, "User Communities: $userCommunities")
                    if (userCommunities.isNotEmpty()) {
                        val userCommunity = userCommunities[0]
                        if (tvPrimeCommunity != null) {
                            tvPrimeCommunity.text = userCommunity.getName()
                        }
                    }
                }
            }
        }
    }

    private fun getUserEvents() {
        val query: ParseQuery<Community> = ParseQuery.getQuery(Community::class.java)
        query.whereEqualTo("isEvent", 1)
        query.include(Community.KEY_OWNER)
        query.whereEqualTo(Community.KEY_OWNER, ParseUser.getCurrentUser())
        query.findInBackground { events, e ->
            if (e != null) {
                // Something went wrong
                Log.e(TAG, "Error fetching events")
            } else {
                if (events != null) {
                    userEvents.addAll(events)
                    Log.i(TAG, "User Events: $userEvents")
                }
            }
        }
    }

    private fun createEvent() {
        if (userEvents.isEmpty()) {
            // Bug fix for accounts that may not have a community owned
            if (userCommunities.isNotEmpty()) {
                val userCommunity = userCommunities[0]
                allCommunities.shuffle()
                val selectedCommunity = allCommunities[0]
                val event = Community()
                event.setOwner(ParseUser.getCurrentUser())
                event.setIsEvent(1)
                event.setMxe1(userCommunity)
                event.setMxe2(selectedCommunity)
                event.setName("${userCommunity.getName()} Meets ${selectedCommunity.getName()}")
                event.saveInBackground { exception ->
                    Log.i(TAG, "Created")
                    if (exception != null) {
                        Log.e(TAG, "Error while saving event")
                        exception.printStackTrace()
                    } else {
                        Log.i(TAG, "Successfully saved event")
                        Log.i(TAG, "Event: $event")
                        userEvents.add(event)
                        Toast.makeText(requireContext(), "Created mxe", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "No owned communities", Toast.LENGTH_SHORT).show()
            }
        }
        else {
            Toast.makeText(requireContext(), "Only one mxe at a time", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "ProfileFragment"
    }
}

