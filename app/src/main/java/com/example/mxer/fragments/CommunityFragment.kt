package com.example.mxer.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxer.Community
import com.example.mxer.Post
import com.example.mxer.PostsAdapter
import com.example.mxer.R
import com.parse.FindCallback
import com.parse.ParseException
import com.parse.ParseQuery

private val TAG = "CommunityFragment"
class CommunityFragment : Fragment() {

    lateinit var rvPosts: RecyclerView
    lateinit var adapter: PostsAdapter
    val allPosts = ArrayList<Post>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragmentManager: FragmentManager = parentFragmentManager
        view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnCompose).setOnClickListener{
            val fragmentToShow = ComposeFragment()
            fragmentManager.beginTransaction().replace(R.id.flContainer, fragmentToShow).commit()
        }
        rvPosts = view.findViewById(R.id.rvPosts)
        adapter = PostsAdapter(requireContext(), allPosts)
        rvPosts.adapter = adapter
        rvPosts.layoutManager = LinearLayoutManager(requireContext())

        val bundle: Bundle = this.requireArguments()
        val communityName: String = bundle.getString("Name","")
        view.findViewById<TextView>(R.id.tvCommTitle).text = communityName
        val communityId: String = bundle.getString("CommunityId","")
        queryPosts(communityId)
    }
    open fun queryPosts(commId: String) {
        val query: ParseQuery<Post> = ParseQuery.getQuery(Post::class.java)
        query.include(Post.KEY_AUTHOR)
        query.addDescendingOrder("createdAt")
        query.limit = 20
        //TODO fix query so it only finds posts under the selected community
        //query.whereEqualTo(Post.KEY_COMM, commId)
        query.findInBackground(object : FindCallback<Post>{
            override fun done(posts: MutableList<Post>?, e: ParseException?) {
                if(e != null) {
                    Log.e(TAG, "Error fetching posts")
                } else {
                    if(posts != null) {
                        allPosts.clear()
                        allPosts.addAll(posts)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }
}