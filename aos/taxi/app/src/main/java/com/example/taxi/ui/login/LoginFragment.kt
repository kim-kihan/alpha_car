package com.example.taxi.ui.login

import android.content.Intent
import android.util.Log
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.taxi.R
import com.example.taxi.base.BaseFragment
import com.example.taxi.databinding.FragmentLoginBinding
import com.example.taxi.di.ApplicationClass
import com.example.taxi.utils.constant.UiState
import com.example.taxi.utils.constant.hide
import com.example.taxi.utils.constant.isValidEmail
import com.example.taxi.utils.constant.show
import com.example.taxi.utils.view.toast
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import java.util.*


@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>(R.layout.fragment_login) {
    val authViewModel: AuthViewModel by viewModels()
    // Google Login
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_GOOGLE_SIGN_IN = 9001
    // Facebook Login 결과를 가져오는 콜백
    private var callbackManager = CallbackManager.Factory.create()

    override fun init() {
        initData()
        setOnClickListeners()
        observerData()
    }

    private fun initData() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.googleRequestIdToKen))
            .requestEmail()
            .build()
        // Google Login
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun setOnClickListeners(){
        binding.buttonLoginLogin.setOnClickListener {
            if (validation()) {
                authViewModel.login(
                    email = binding.editTextEmailLoginId.text.toString(),
                    password = binding.editTextEmailLoginPw.text.toString()
                )
            }
        }
        binding.imageLoginGithub.setOnClickListener{
            authViewModel.githubLogin(
                activity = requireActivity()
            )
        }
        binding.imageLoginFacebook.setOnClickListener{
//            facebookLogin()
        }
        binding.imageLoginGoogle.setOnClickListener{
            googleSignIn()
        }
        binding.textEmailLoginFindPW.setOnClickListener{
            findNavController().navigate(R.id.action_loginFragment_to_findPWFragment)
        }
        binding.textLoginSignup.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_joinFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        authViewModel.getSession { user ->
            if (user != null){
                ApplicationClass.userId = user.userId
                ApplicationClass.prefs.name = user.name
                ApplicationClass.prefs.userSeq = user.userSeq
                ApplicationClass.prefs.useCount = user.useCount
                ApplicationClass.prefs.profileImage = user.profileImage
                ApplicationClass.prefs.tel = user.tel
                ApplicationClass.prefs.isEachProvider = user.isEachProvider
                if(user.isEachProvider){
                    ApplicationClass.prefs.providerId = user.userId
                }
                findNavController().navigate(R.id.action_loginFragment_to_userHomeFragment)
            }
        }
    }
    private fun observerData() {
        authViewModel.login.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarLoginLoading.show()
                }
                is UiState.Failure -> {
                    binding.progressBarLoginLoading.hide()
                    state.error?.let {
                        toast(it)
                        Log.d("UiState.Failure", it)
                    }
                }
                is UiState.Success -> {
                    binding.progressBarLoginLoading.hide()
                    authViewModel.getSession { user ->
                        Log.d("user : ", user.toString())
                        if (user != null) {
                            ApplicationClass.userId = user.userId
                            ApplicationClass.prefs.name = user.name
                            ApplicationClass.prefs.tel = user.tel
                            ApplicationClass.prefs.userSeq = user.userSeq
                            ApplicationClass.prefs.useCount = user.useCount
                            ApplicationClass.prefs.profileImage = user.profileImage
                            ApplicationClass.prefs.isEachProvider = user.isEachProvider
                            if(user.isEachProvider){
                                ApplicationClass.prefs.providerId = user.userId
                            }
                            findNavController().navigate(R.id.action_loginFragment_to_userHomeFragment)
                        }
                    }
                }
            }
        }
        authViewModel.googleLogin.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarLoginLoading.show()
                }
                is UiState.Failure -> {
                    binding.progressBarLoginLoading.hide()
                    state.error?.let {
                        toast(it)
                        Log.d("UiState.Failure", it)
                    }
                }
                is UiState.Success -> {
                    binding.progressBarLoginLoading.hide()
                    println("state.data : " + state.data)
                    if(state.data == "null"){
                        findNavController().navigate(R.id.action_loginFragment_to_joinFragment)
                    }else{
                        //binding.progressBar.hide()
                        authViewModel.getSession { user ->
                            Log.d("user : ", user.toString())
                            if (user != null) {
                                if(user.tel != ""){
                                    ApplicationClass.userId = user.userId
                                    ApplicationClass.prefs.name = user.name
                                    ApplicationClass.prefs.tel = user.tel
                                    ApplicationClass.prefs.userSeq = user.userSeq
                                    ApplicationClass.prefs.useCount = user.useCount
                                    ApplicationClass.prefs.profileImage = user.profileImage
                                    if(user.isEachProvider){
                                        ApplicationClass.prefs.providerId = user.userId
                                    }
                                    findNavController().navigate(R.id.action_loginFragment_to_userHomeFragment)
                                }else{
                                    findNavController().navigate(R.id.action_loginFragment_to_joinFragment)
                                }
                            }else{
                                findNavController().navigate(R.id.action_loginFragment_to_joinFragment)
                            }
                        }
                    }
                }
            }
        }

        authViewModel.githubLogin.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarLoginLoading.show()
                }
                is UiState.Failure -> {
                    binding.progressBarLoginLoading.hide()
                    state.error?.let {
                        toast(it)
                        Log.d("UiState.Failure", it)
                    }
                }
                is UiState.Success -> {
                    binding.progressBarLoginLoading.hide()
                    println("state.data : " + state.data)
                    if(state.data == "null"){
                        findNavController().navigate(R.id.action_loginFragment_to_joinFragment)
                    }else{
                        //binding.progressBar.hide()
                        authViewModel.getSession { user ->
                            Log.d("user : ", user.toString())
                            if (user != null) {
                                if(user.tel != ""){
                                    ApplicationClass.userId = user.userId
                                    ApplicationClass.prefs.name = user.name
                                    ApplicationClass.prefs.tel = user.tel
                                    ApplicationClass.prefs.userSeq = user.userSeq
                                    ApplicationClass.prefs.useCount = user.useCount
                                    ApplicationClass.prefs.profileImage = user.profileImage
                                    findNavController().navigate(R.id.action_loginFragment_to_userHomeFragment)
                                }else{
                                    findNavController().navigate(R.id.action_loginFragment_to_joinFragment)
                                }
                            }else{
                                findNavController().navigate(R.id.action_loginFragment_to_joinFragment)
                            }
                        }
                    }
                }
            }
        }

        authViewModel.facebookLogin.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    //binding.progressBar.show()
                }
                is UiState.Failure -> {
                    //binding.progressBar.hide()
                    state.error?.let {
                        toast(it)
                        Log.d("UiState.Failure", it)
                    }
                }
                is UiState.Success -> {
                    println("state.data : " + state.data)
                    if(state.data == "null"){
                        findNavController().navigate(R.id.action_loginFragment_to_joinFragment)
                    }else{
                        //binding.progressBar.hide()
                        authViewModel.getSession { user ->
                            Log.d("user : ", user.toString())
                            if (user != null) {
                                if(user.tel != ""){
                                    ApplicationClass.userId = user.userId
                                    ApplicationClass.prefs.name = user.name
                                    ApplicationClass.prefs.tel = user.tel
                                    ApplicationClass.prefs.userSeq = user.userSeq
                                    ApplicationClass.prefs.useCount = user.useCount
                                    ApplicationClass.prefs.profileImage = user.profileImage
                                    findNavController().navigate(R.id.action_loginFragment_to_userHomeFragment)
                                }else{
                                    findNavController().navigate(R.id.action_loginFragment_to_joinFragment)
                                }
                            }else{
                                findNavController().navigate(R.id.action_loginFragment_to_joinFragment)
                            }
                        }
                    }
                }
            }
        }
    }
    private fun validation(): Boolean {
        var isValid = true

        if (binding.editTextEmailLoginId.text.isNullOrEmpty()){
            isValid = false
            toast("이메일을 입력해주세요.")
        }else{
            if (!binding.editTextEmailLoginId.text.toString().isValidEmail()){
                isValid = false
                toast("이메일 양식에 맞게 다시 입력해 주세요.")
            }
        }
        if (binding.editTextEmailLoginPw.text.isNullOrEmpty()){
            isValid = false
            toast("비밀번호를 입력해주세요.")
        }else{
            if (binding.editTextEmailLoginPw.text.toString().length < 6){
                isValid = false
                toast("비밀번호를 6자리 이상 입력해주세요.")
            }
        }
        return isValid
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        /** 구글 로그인 Start**/
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d("GoogleActivity", "firebaseAuthWithGoogle:" + account.id)
                val tmp = account.idToken!!.split(".")
                ApplicationClass.prefs.userSeq = tmp[0]
                authViewModel.googleLogin(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("GoogleActivity", "Google sign in failed", e)
            }
        }
        /** 구글 로그인 End**/
        /** 페이스북 로그인 Start**/
        else {
            callbackManager?.onActivityResult(requestCode, resultCode, data)
        }
        /** 페이스북 로그인 End**/
    }

    /** 구글 로그인 Start**/
    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }
    /** 구글 로그인 End**/

    /** 페이스북 로그인 Start**/
    private fun facebookLogin() {
        println("facebook: 페북 로그인 시작")
        // 페이스북에서 받을 권한 요청 - 프로필 이미지, 이메일
        LoginManager.getInstance()
//            .logInWithReadPermissions(this, Arrays.asList("email", "public_profile", "user_friends"))
//            .logInWithReadPermissions(requireActivity(), callbackManager, Arrays.asList("public_profile", "email"))
            .logInWithReadPermissions(requireActivity(), Arrays.asList("public_profile", "email"))
        println("facebook: 권한 완료")

        LoginManager.getInstance()
            .registerCallback(callbackManager, object:FacebookCallback<LoginResult>{
                override fun onSuccess(result: LoginResult) {
                    println("facebook: result : " + result)
                    println("facebook: result?accessToken : " + result?.accessToken)
                    if (result?.accessToken != null) {
                        // facebook 계정 정보를 firebase 서버에게 전달(로그인)
                        val accessToken = result.accessToken
                        authViewModel.facebookLogin(accessToken)
                        Log.d("facebook 로그인", "성공")
                    } else {
                        Log.d("Facebook", "Fail Facebook Login")
                    }
                }
                override fun onCancel() {
                    //취소가 된 경우 할일
                    println("facebook: 로그인 취소")
                    Log.d("facebook 로그인", "취소")
                }
                override fun onError(error: FacebookException) {
                    //에러가 난 경우 할일
                    println("facebook: 에러 발생")
                    Log.d("facebook 로그인", "에러 발생 $error")
                }
            })
    }
    /** 페이스북 로그인 End**/
}
