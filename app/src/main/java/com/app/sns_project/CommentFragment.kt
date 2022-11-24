package com.app.sns_project

import android.content.ContentValues.TAG
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Im
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.sns_project.model.ContentDTO
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.app.sns_project.databinding.CommentFragmentBinding
import com.app.sns_project.util.pushMessage
import com.google.firebase.firestore.DocumentSnapshot

class CommentFragment : Fragment() { //R.layout.comment_fragment

    private lateinit var binding: CommentFragmentBinding
    var contentUid : String? = null
    private lateinit var myAdapter : CommentFragment.CommentRecyclerviewAdapter
    var comments : ArrayList<ContentDTO.Comment> = arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
           binding = CommentFragmentBinding.inflate(inflater, container, false)

        Log.d("onCreateView", "zzz")
        val args:CommentFragmentArgs by navArgs()
        contentUid = args.contentId
        Log.d("contentUid in onCreateView", contentUid!!)

        val commentRecyclerView = binding.commentRecyclerview
        myAdapter = CommentFragment().CommentRecyclerviewAdapter(comments, contentUid)
        commentRecyclerView.adapter = myAdapter
        //commentRecyclerView.adapter  = CommentFragment().CommentRecyclerviewAdapter()
        commentRecyclerView.layoutManager = LinearLayoutManager(context) // activity?
        commentRecyclerView.setHasFixedSize(true)

        return binding.root
        //return v
    }

    override fun onResume() {
        super.onResume()
        commentLoading()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("contentUid in onViewCreated", contentUid!!)
        Log.d("onViewCreated", this::binding.toString())
        val commentSendButton = binding.commentSendButton
        var name : String? = null
        commentSendButton.setOnClickListener {
            var comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            //comment.userName = FirebaseAuth.getInstance().currentUser?.displayName // 유저 이름
            FirebaseFirestore.getInstance().collection("user")
                .document(comment.uid!!)
                .get().addOnSuccessListener { document ->
                    if (document != null) {
                        comment.userName = document.get("userName").toString() // ??null??
                        Log.d("userName", comment.userName!!)
                    } else {
                        Log.d(TAG, "No such document")
                    }
                    FirebaseFirestore.getInstance().collection("post").document(contentUid!!)
                        .collection("comments").document().set(comment)
                }
            val commentEditText = binding.commentEditText
            comment.comment = commentEditText.text.toString()
            comment.timestamp = System.currentTimeMillis()


            commentEditText.setText("")
            FirebaseFirestore.getInstance().collection("post").document(contentUid!!)
                .get().addOnSuccessListener {
                    Log.d("PostUserName", it.get("userName").toString())
                    commentAlarm(it.get("userName").toString())
                }

        }

        //val userName = v.findViewById<TextView>(R.id.commentViewUserName) // 게시글을 올린 유저의 이름
        val userName = binding.commentViewUserName
        var postUserId: String? = null // 게시글을 올린 유저의 uid
        //FirebaseFirestore.getInstance().collection("post").document(contentUid!!)
//        var userContent = v.findViewById<TextView>(R.id.my_text) // 게시글 content
        var userContent = binding.myText
        var userImageContent = binding.myImg
        FirebaseFirestore.getInstance().collection("post").document(contentUid!!)
            .get().addOnSuccessListener { document ->
                if (document != null) {
                    userName.text = document.get("userName").toString() // 유저의 이름을 기입
                    postUserId = document.get("uid").toString()
                    Log.d("uid = ", postUserId!!)
                    userContent.text = document.get("content").toString() // 게시물의 content 기입
                    var url : ArrayList<String> = document.get("imageUrl") as ArrayList<String>
                    try{
                        Glide.with(this).load(url[0]).apply(RequestOptions())
                            .into(userImageContent)
                    }catch (e : Exception){
                        Log.d("ddd", "ddd")
                        userImageContent.visibility=View.GONE
//                        val urlException = "https://firebasestorage.googleapis.com/v0/b/snsproject-638d2.appspot.com/o/images%2Fprofile_images%2Fwhite2.jpeg?alt=media&token=14c0cba4-78f1-4b3c-b7d6-b24fc105d3c1"
//                        Glide.with(this).load(urlException).apply(RequestOptions())
//                            .into(userImageContent)
                    }
                    //Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                } else {
                    Log.d(TAG, "No such document")
                }
                val userProfile = binding.postViewProfile // ok
                FirebaseFirestore.getInstance().collection("user").document(postUserId!!).get()
                    .addOnSuccessListener { document ->
                        if(document != null) {
                            var url = document.get("profileImage")
                            Glide.with(this).load(url).apply(RequestOptions().circleCrop())
                                .into(userProfile)
                        }
                    }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
        println("kkkkkkkkkk")

    }

//    private fun logOut() {
//        val uid : String? = null
//        var userId = FirebaseAuth.getInstance().currentUser?.uid
//        val logoutButton = binding.logoutButton
//        if(uid == userId) {
//            logoutButton.setOnclickListener {
//                startActivity(Intent(activity, LoginActivity))
//            }
//        }
//        else {
//            logoutButton.visibility = View.INVISIBLE
//        }
//    }

    private fun commentAlarm(postUseruid:String){
        FirebaseFirestore.getInstance().collection("user").document(FirebaseAuth.getInstance().currentUser!!.uid).get().addOnSuccessListener {
            val userName = it["userName"] as String

            Log.d("userName",userName)
            var message = String.format("%s 님이 댓글을 남겼습니다.",userName)
            pushMessage()?.sendMessage(postUseruid, "알림 메세지 입니다.", message)
        }
    }

    private fun commentLoading(){
        //setAdapter()

        //comments = arrayListOf()
        println("in init $contentUid")
        //contentUid = "98gEZ3ziBphbvDw80KAh" /// *******
        Log.d("contentUid in init", contentUid!!)
        Log.d("in inner class init", "ok")
        FirebaseFirestore.getInstance()
            .collection("post")
            .document(contentUid!!)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { querySnapShot, FirebaseFirestoreException ->
                comments.clear()
                if(querySnapShot == null)
                    return@addSnapshotListener
                for(snapshot in querySnapShot.documents!!) {
                    comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    Log.d("inquerySnapShot", comments.size.toString())
                }
                myAdapter.notifyDataSetChanged() // 리싸이클러 뷰 새로고침
            }
    }

    private fun setAdapter(){
        val commentRecyclerView = binding.commentRecyclerview
        myAdapter = CommentFragment().CommentRecyclerviewAdapter(comments, contentUid)
        commentRecyclerView.adapter = myAdapter
        //commentRecyclerView.adapter  = CommentFragment().CommentRecyclerviewAdapter()
        commentRecyclerView.layoutManager = LinearLayoutManager(context) // activity?
        commentRecyclerView.setHasFixedSize(true);
    }

    inner class CommentRecyclerviewAdapter(var comments: ArrayList<ContentDTO.Comment>, var contentUid : String?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view) {

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView
            view.findViewById<TextView>(R.id.commentViewComment).text = comments[position].comment
            view.findViewById<TextView>(R.id.commentViewUserID).text = comments[position].userName // userId로 해도됨
            FirebaseFirestore.getInstance().collection("user")
                .document(comments[position].uid!!)
                .get()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        var url = task.result!!["profileImage"]
                        Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop())
                            .into(view.findViewById(R.id.commentViewProfile))
                    }
                }

            val deleteButton = view.findViewById<ImageButton>(R.id.deleteButton)

            FirebaseFirestore.getInstance()
                .collection("post")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp").addSnapshotListener { value, error ->
                    if(value == null) return@addSnapshotListener
                    val commUid = value.documents[position].get("uid")
                    if(commUid != FirebaseAuth.getInstance().currentUser?.uid){
                        deleteButton.visibility = View.INVISIBLE
                    }
                }

            deleteButton.setOnClickListener {
//                comments.removeAt(position)
//                notifyItemRemoved(position)
//                notifyItemRangeChanged(position, comments.size)
                //notifyDataSetChanged()
                FirebaseFirestore.getInstance()
                    .collection("post")
                    .document(contentUid!!)
                    .collection("comments")
                    .orderBy("timestamp").addSnapshotListener { value, error ->
                        if(value == null) return@addSnapshotListener
                        val comm = value.documents[position]
                        Log.d("position", position.toString())
                        val commentId = comm.id
                        Log.d("comment doc Id",commentId)
                        FirebaseFirestore.getInstance()
                            .collection("post")
                            .document(contentUid!!)
                            .collection("comments")
                            .document(commentId)
                            .delete()
                            .addOnSuccessListener {
                                //notifyItemRangeChanged(position, comments.size-1)
                                Log.d("delete", position.toString())
                            }
                            .addOnFailureListener {
                                Log.d("error", it.toString())
                            }
                    }

            }
        }

        override fun getItemCount(): Int {
            Log.d("getItemCount", comments.size.toString())
            return comments.size
        }

    }
}