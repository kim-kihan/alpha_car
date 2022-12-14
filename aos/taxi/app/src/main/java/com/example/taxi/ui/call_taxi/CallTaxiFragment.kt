package com.example.taxi.ui.call_taxi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taxi.R
import com.example.taxi.base.BaseFragment
import com.example.taxi.data.api.KakaoAPI
import com.example.taxi.data.dto.user.destination.Destination
import com.example.taxi.data.dto.user.destination.DestinationSearch
import com.example.taxi.data.dto.user.destination.DestinationSearchDto
import com.example.taxi.data.dto.user.route.Location
import com.example.taxi.data.dto.user.route.RouteSetting
import com.example.taxi.databinding.FragmentCallTaxiBinding
import com.example.taxi.di.ApplicationClass
import com.example.taxi.ui.call_taxi.setting.DestinationSearchListAdapter
import com.example.taxi.utils.constant.KakaoApi
import com.example.taxi.utils.constant.UiState
import com.example.taxi.utils.constant.hide
import com.example.taxi.utils.constant.show
import com.example.taxi.utils.view.toast
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.ssafy.daero.utils.view.getPxFromDp
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@AndroidEntryPoint
class CallTaxiFragment : BaseFragment<FragmentCallTaxiBinding>(R.layout.fragment_call_taxi),
    OnMapReadyCallback {

    private val callTaxiViewModel : CallTaxiViewModel by viewModels()
    private lateinit var startingPoint : Destination
    private lateinit var destination : Destination
    private lateinit var destinationSearchListAdapter: DestinationSearchListAdapter
    var checkState = true
    private var naverMap: NaverMap? = null
    private var uiSettings: UiSettings? = null
    private var markers = mutableListOf<Marker>()
    private var paths = mutableListOf<PathOverlay>()
    private var distance = 0.0

    private val destinationSearchClickListener: (View, String, String, String, String) -> Unit = { _, place, address, x, y ->
        binding.searchCallTaxi.visibility = View.GONE
        binding.recyclerviewCallTaxiSearch.visibility = View.GONE
        binding.textCallTaxiStart.visibility = View.VISIBLE
        binding.imageCallTaxiForward.visibility = View.VISIBLE
        binding.textCallTaxiDestination.visibility = View.VISIBLE
        binding.searchCallTaxi.setQuery("", false)
        destination = Destination(address,y,place,x)
        binding.textCallTaxiDestination.text = destination.addressName
        checkEnd()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        initNaverMap()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun init() {
        initData()
        observerData()
        setOnClickListeners()
    }

    private fun initData() {
        if(arguments?.getParcelable<Destination>("Destination")!=null && arguments?.getParcelable<Destination>("StartingPoint")!=null){
            destination = arguments?.getParcelable<Destination>("Destination") as Destination
            Log.d("?????????",destination.toString())
            startingPoint = arguments?.getParcelable<Destination>("StartingPoint") as Destination
            Log.d("?????????",startingPoint.toString())
            binding.textCallTaxiDestination.text = destination.addressName
            binding.textCallTaxiStart.text = startingPoint.addressName
        }
    }

    private fun observerData(){
        callTaxiViewModel.routeSetting.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
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
                    toast("????????? ?????? ????????????. ????????? ????????? ?????????.")
                    callTaxiViewModel.getRoute()
                }
            }
        }
        callTaxiViewModel.route.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
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
                    val json = requireActivity().assets.open("node_set.json").reader().readText()
                    val node = JSONObject(json)
                    val location = mutableListOf<Location>()
                    for(i in state.data){
                        val pathNode = node.getJSONArray(i)
                        location.add(Location(pathNode.getDouble(1).toString(), pathNode.getDouble(0).toString()))
                    }
                    callTaxiViewModel.getDistance()
                    deleteMarkers()
                    deletePaths()
                    drawMarkers(location)
                    drawPolyline(location)
                }
            }
        }
        callTaxiViewModel.distance.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
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
                    var str = ((state.data.toDouble()/1000.0) * 100.0).roundToInt() / 100.0
                    ApplicationClass.prefs.distanceStart = str.toFloat()
                    binding.textCallTaxiDistance.text = str.toString() +"Km"
                    ApplicationClass.prefs.distance = str.toFloat()
                    binding.textCallTaxiCash.text = "1,000 ???"
                    //getFee(str)
                }
            }
        }
    }

    private fun getFee(distance: Double){
        var distance = distance
        val numberFormat: NumberFormat = NumberFormat.getInstance()
        if(distance <= 3){
            val str = numberFormat.format(3000)
            binding.textCallTaxiCash.text = str + " ???"
        }else{
            distance -= 3
            var res = distance/0.16
            var fee = 3000 + (res.toInt()*100)
            val str = numberFormat.format(fee)
            binding.textCallTaxiCash.text = str + " ???"
        }
    }

    private fun deleteMarkers() {
        if (markers.isNotEmpty()) {
            markers.forEach {
                it.map = null
            }
        }
        markers = mutableListOf()
    }

    private fun deletePaths() {
        if (paths.isNotEmpty()) {
            paths.forEach {
                it.map = null
            }
        }
        paths = mutableListOf()
    }

    private fun drawMarkers(location: List<Location>) {
        if (location.isNotEmpty()) {
            markers.add(createMarker(location[0], 0))
            markers.add(createMarker(location[location.lastIndex], location.lastIndex))

            naverMap?.moveCamera(
                CameraUpdate.scrollTo(
                    LatLng(
                        location[0].lati.toDouble(),
                        location[0].long.toDouble()
                    )
                )
            )
            naverMap?.moveCamera(CameraUpdate.zoomTo(15.0))
        }
    }

    private fun createMarker(location: Location, index: Int): Marker {
        return Marker().apply {
            position = LatLng(location.lati.toDouble(), location.long.toDouble())    // ?????? ??????
            icon = OverlayImage.fromResource(R.drawable.ic_marker)
            if(index==0){
                iconTintColor = requireActivity().getColor(R.color.greenTextColor)// ?????? ??????
            }else{
                iconTintColor = requireActivity().getColor(R.color.primaryColor)// ?????? ??????
            }
            width = requireContext().getPxFromDp(40f)   // ?????? ?????? ??????
            height = requireContext().getPxFromDp(40f)  // ?????? ?????? ??????
            zIndex = 0  // ?????? ??????
            onClickListener = Overlay.OnClickListener {     // ?????? ?????? ?????????
                return@OnClickListener true
            }
            isHideCollidedMarkers = true    // ????????? ?????? ?????? ?????????
            map = naverMap  // ????????? ?????? ??????
        }
    }

    private fun drawPolyline(location: List<Location>) {
        if (location.isNotEmpty()) {
            if (location.size >= 2) {  // ??? ???????????? ??? ??? ?????? ????????? ???????????? ?????? ?????? ?????????
                paths.add(PathOverlay().apply {
                    color = requireActivity().getColor(R.color.primaryColor) // ?????? ??????
                    outlineColor = requireActivity().getColor(R.color.primaryColor) // ?????? ??????
                    outlineWidth = requireContext().getPxFromDp(1.5f) // ?????? ??????
                    var list = mutableListOf<LatLng>()
                    for(i in location){
                        list.add(LatLng(i.lati.toDouble(), i.long.toDouble()))
                    }
                    coords = list  // ?????? ??????
                    map = naverMap
                })
            }
        }
    }

    private fun setOnClickListeners() {
        binding.textCallTaxiStart.setOnClickListener {
            findNavController().navigate(R.id.action_callTaxiFragment_to_startPointSettingFragment
                ,bundleOf("Destination" to destination))
        }
        binding.textCallTaxiDestination.setOnClickListener {
            binding.searchCallTaxi.visibility = View.VISIBLE
            binding.recyclerviewCallTaxiSearch.visibility = View.VISIBLE
            binding.textCallTaxiStart.visibility = View.GONE
            binding.imageCallTaxiForward.visibility = View.GONE
            binding.textCallTaxiDestination.visibility = View.GONE
            checkState = true
        }
        binding.imageCallTaxiLatePayment.setOnClickListener {
            findNavController().navigate(R.id.action_callTaxiFragment_to_waitingCallTaxiFragment,
                bundleOf("Destination" to destination, "StartingPoint" to startingPoint)
            )
        }
        binding.textCallTaxiLatePayment.setOnClickListener {
            findNavController().navigate(R.id.action_callTaxiFragment_to_waitingCallTaxiFragment,
                bundleOf("Destination" to destination, "StartingPoint" to startingPoint)
            )
        }
        binding.searchCallTaxi.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // ?????? ?????? ?????? ??? ??????

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null && newText != "") {
                    searchKeyword(newText)
                }
                return true
            }
        })
        binding.imageCallTaxiBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun searchKeyword(keyword: String) {
        val retrofit = Retrofit.Builder()   // Retrofit ??????
            .baseUrl(KakaoApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(KakaoAPI::class.java)   // ?????? ?????????????????? ????????? ??????
        val call = api.getSearchKeyword(KakaoApi.API_KEY, keyword)   // ?????? ?????? ??????

        // API ????????? ??????
        call.enqueue(object: Callback<DestinationSearchDto> {
            override fun onResponse(
                call: Call<DestinationSearchDto>,
                response: Response<DestinationSearchDto>
            ) {
                // ?????? ?????? (?????? ????????? response.body()??? ????????????)
                Log.d("Test", "Raw: ${response.raw()}")
                Log.d("Test", "Body: ${response.body()}")
                if(response.body()!!.documents != null){
                    if(checkState){
                        getDistance(response.body()!!.documents)
                    }else{
                        initSearchAdapter(response.body()!!.documents)
                    }
                }
            }

            override fun onFailure(call: Call<DestinationSearchDto>, t: Throwable) {
                // ?????? ??????
                Log.w("MainActivity", "?????? ??????: ${t.message}")
            }
        })
    }

    private fun getDistance(list : List<DestinationSearch>) {
        if(startingPoint!=null){
            for(i in list){
                var newX = ( kotlin.math.cos(startingPoint.latitude.toDouble()) * 6400 * 2 * 3.14 / 360 ) * abs(startingPoint.longitude.toDouble() - i.x.toDouble())
                var newY = 111 * abs(startingPoint.longitude.toDouble() - i.y.toDouble())

                i.distance = ((sqrt(newX.pow(2)+newY.pow(2)) * 100.0).roundToInt() / 100.0).toString()+"Km"
            }
        }
        initSearchAdapter(list)
    }

    private fun initSearchAdapter(list : List<DestinationSearch>){
        destinationSearchListAdapter = DestinationSearchListAdapter().apply {
            onItemClickListener = destinationSearchClickListener
        }
        binding.recyclerviewCallTaxiSearch.apply {
            adapter = destinationSearchListAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        }
        destinationSearchListAdapter.updateList(list)
    }
    
    private fun checkEnd(){
        if(binding.textCallTaxiStart.text !="" && binding.textCallTaxiDestination.text != ""){
            callTaxiViewModel.addRouteSetting(RouteSetting(destination = Location(
                lati = destination.latitude, long = destination.longitude),
                startingPoint = Location(lati = startingPoint.latitude, long = startingPoint.longitude)))
            callTaxiViewModel.getRoute()
        }
    }

    private fun initNaverMap() {
        val _naverMap =
            childFragmentManager.findFragmentById(R.id.fragmentContainer_call_taxi) as MapFragment?
                ?: MapFragment.newInstance().also {
                    childFragmentManager.beginTransaction()
                        .add(R.id.fragmentContainer_call_taxi, it)
                        .commit()
                }
        _naverMap.getMapAsync(this)
    }

    override fun onMapReady(_naverMap: NaverMap) {
        naverMap = _naverMap

        setNaverMapUI()
        getRoute()
    }

    private fun setNaverMapUI() {
        naverMap?.apply {
            isLiteModeEnabled = false // ????????? ?????? ?????? (?????? ?????? ?????? ?????? X)

            this@CallTaxiFragment.uiSettings = this.uiSettings.apply {
                isCompassEnabled = false // ????????? ????????????
                isZoomControlEnabled = false // ?????? ?????? ?????? ????????????
                isScaleBarEnabled = false // ????????? ??? ????????????
                isLocationButtonEnabled = false // ?????? ??? ?????? ?????? ????????????
            }
        }
    }

    private fun getRoute() {
        callTaxiViewModel.getDistance()
        if(arguments?.getParcelable<Destination>("Destination")!=null && arguments?.getParcelable<Destination>("StartingPoint")!=null){
            destination = arguments?.getParcelable<Destination>("Destination") as Destination
            startingPoint = arguments?.getParcelable<Destination>("StartingPoint") as Destination
            callTaxiViewModel.addRouteSetting(RouteSetting(destination = Location(
                lati = destination.latitude, long = destination.longitude),
            startingPoint = Location(lati = startingPoint.latitude, long = startingPoint.longitude)))
        }
    }

}