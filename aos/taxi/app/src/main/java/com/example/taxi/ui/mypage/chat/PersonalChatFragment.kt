package com.example.taxi.ui.mypage.chat

import android.os.Handler
import android.util.Log
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taxi.R
import com.example.taxi.base.BaseFragment
import com.example.taxi.data.dto.mypage.Chat
import com.example.taxi.data.dto.mypage.ChatModel
import com.example.taxi.data.dto.user.boarded_taxi_list.BoardedTaxi
import com.example.taxi.databinding.FragmentPersonalChatBinding
import com.example.taxi.di.ApplicationClass
import com.example.taxi.ui.mypage.favorites.UpdateFavoritesAdapter
import com.example.taxi.utils.constant.UiState
import com.example.taxi.utils.constant.hide
import com.example.taxi.utils.constant.show
import com.example.taxi.utils.view.toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import dagger.hilt.android.AndroidEntryPoint
import org.w3c.dom.Comment
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PersonalChatFragment : BaseFragment<FragmentPersonalChatBinding>(R.layout.fragment_personal_chat) {
    private val personalChatViewModel : PersonalChatViewModel by viewModels()

    private lateinit var personalChatAdapter : PersonalChatAdapter
    private var comments = ArrayList<ChatModel.CommentModel>()
    private var chatRoomUid : String? = null
    private var startUserName : String? = null
    private var destinationUserImg : String? = null
    private var destinationUserName : String? = null

    override fun init() {
        initAdapter()
        initData()
        setOnClickListeners()
        observerData()
    }

    private fun initAdapter() {
        personalChatAdapter = PersonalChatAdapter()

        binding.messageActivityRecyclerview.apply {
            adapter = personalChatAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        }
    }

    private fun initData() {
        startUserName = getName(arguments?.getString("startUserName") as String)

        ApplicationClass.prefs.destinationUserName = arguments?.getString("destinationUserName") as String
        binding.textPersonalChatUserName.text = ApplicationClass.prefs.destinationUserName
        destinationUserName = getName(ApplicationClass.prefs.destinationUserName.toString())

        ApplicationClass.prefs.destinationUserImg = arguments?.getString("destinationUserImg") as String
        destinationUserImg = ApplicationClass.prefs.destinationUserImg.toString()


        personalChatViewModel.checkChatRoom(
            startUserName = startUserName!!,
            destinationUserName = destinationUserName!!
        )
    }
    private fun createChatRooms() {
        if (chatRoomUid == null) {
            println("        if (chatRoomUid == null)")
            val chatModel = ChatModel()
            chatModel.users.put(startUserName.toString(), true)
            chatModel.users.put(destinationUserName.toString(), true)

            personalChatViewModel.createChatRooms(
                chatModel = chatModel
            )
        }else{
            // ????????? ??????
            personalChatViewModel.getComment(
                destinationUserName = destinationUserName!!,
                chatRoomUid = chatRoomUid!!
            )
        }
    }

    private fun setOnClickListeners() {
        binding.imagePersonalChatBack.setOnClickListener{
            requireActivity().onBackPressed()
        }
        binding.imagePersonalChat.setOnClickListener{

            if(binding.edittextPersonalChat.text.toString() != "") {
                binding.imagePersonalChat.setAlpha(128)
                binding.imagePersonalChat.isEnabled = false

                println("chatRoomUid: " + chatRoomUid)
                //????????? ?????????
                // ?????? ??????
                val time = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("MM??? dd??? hh:mm")
                val curTime = dateFormat.format(Date(time)).toString()

                val comment = ChatModel.CommentModel(
                    startUserName,
                    binding.edittextPersonalChat.text.toString(),
                    curTime
                )
                // ????????? ??????
                Handler().postDelayed({
                    personalChatViewModel.addComment(
                        chatRoomUid = chatRoomUid!!,
                        comment = comment
                    )
                    binding.edittextPersonalChat.text = null
                    binding.imagePersonalChat.isEnabled = true
                    binding.imagePersonalChat.setAlpha(255)
                }, 1000L)

                // ????????? ??????
                personalChatViewModel.getComment(
                    destinationUserName = destinationUserName!!,
                    chatRoomUid = chatRoomUid!!
                )
            }else{
                toast("???????????? ??????????????????.")
            }
        }
    }

    private fun observerData() {
        personalChatViewModel.createChatRooms.observe(viewLifecycleOwner){ state ->
            when(state){
                is UiState.Loading ->{
                    binding.progressBar.show()
                }
                is UiState.Failure -> {
                    binding.progressBar.hide()
                    state.error?.let {
                        toast(it)
                        Log.d("UiState.Failure", it)
                    }
                }
                is UiState.Success -> {
                    binding.progressBar.hide()
                    personalChatViewModel.checkChatRoom(
                        startUserName = startUserName!!,
                        destinationUserName = destinationUserName!!
                    )
                }
            }
        }

        personalChatViewModel.checkChatRoom.observe(viewLifecycleOwner){ state ->
            when(state){
                is UiState.Loading ->{
                    binding.progressBar.show()
                }
                is UiState.Failure -> {
                    binding.progressBar.hide()
                    state.error?.let {
                        toast(it)
                        Log.d("UiState.Failure", it)
                    }
                }
                is UiState.Success -> {
                    binding.progressBar.hide()
                    for(data in state.data.children){
                        val item = data.getValue<ChatModel>()

                        if(item?.users!!.containsKey(destinationUserName)){
                            chatRoomUid = data.key
                            println("chatRoomUid: " + chatRoomUid)
                        }
                    }
                    createChatRooms()
                }
            }
        }

        personalChatViewModel.getComment.observe(viewLifecycleOwner){ state ->
            when(state){
                is UiState.Loading ->{
                    binding.progressBar.show()
                }
                is UiState.Failure -> {
                    binding.progressBar.hide()
                    state.error?.let {
                        toast(it)
                        Log.d("UiState.Failure", it)
                    }
                }
                is UiState.Success -> {
                    binding.progressBar.hide()
                    comments.clear()
                    for(data in state.data.children){
                        val item = data.getValue<ChatModel.CommentModel>()
                        comments.add(item!!)
                        println(comments)
                    }
                    personalChatAdapter.updateList(comments)
                    binding.messageActivityRecyclerview.scrollToPosition(personalChatAdapter.itemCount-1)
                }
            }
        }
    }
    private fun getName(userName: String) : String {
        var result = userName
        // email Set?????? ???????????? ????????? ???????????? .??? @??? ?????????.
        result = result!!.replace(".", "").replace("@", "")
        return result
    }
}
