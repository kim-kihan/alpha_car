package com.example.taxi.ui.driving

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.taxi.R
import com.example.taxi.base.BaseFragment
import com.example.taxi.data.dto.user.driving.CurrentLocation
import com.example.taxi.data.dto.user.route.Location
import com.example.taxi.data.dto.user.route.RouteSetting
import com.example.taxi.databinding.FragmentDrivingTaxiBinding
import com.example.taxi.di.ApplicationClass
import com.example.taxi.ui.call_taxi.CallTaxiViewModel
import com.example.taxi.ui.call_taxi.location.MarkerInfoAdapter
import com.example.taxi.utils.constant.UiState
import com.example.taxi.utils.constant.hide
import com.example.taxi.utils.constant.show
import com.example.taxi.utils.view.toast
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.*
import com.ssafy.daero.utils.view.getPxFromDp
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

@AndroidEntryPoint
class DrivingTaxiFragment : BaseFragment<FragmentDrivingTaxiBinding>(R.layout.fragment_driving_taxi),
    OnMapReadyCallback {

    private val callTaxiViewModel : CallTaxiViewModel by viewModels()
    var distance = 0
    var preDistance = 0
    private var naverMap: NaverMap? = null
    private var uiSettings: UiSettings? = null
    private var markers = mutableListOf<Marker>()
    private var paths = mutableListOf<PathOverlay>()
    private var infoWindow = InfoWindow()
    private var rootView: ViewGroup? = null
    private var fee = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = container
        initNaverMap()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun init() {
        initView()
        observerData()
    }

    private fun initView(){
        binding.textDrivingTaxiName.text = ApplicationClass.prefs.carName
        Glide.with(requireContext())
            .load(ApplicationClass.prefs.carImage)
            .into(binding.imageDrivingTaxiCar)
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
                    toast("경로를 설정 중입니다. 잠시만 기다려 주세요.")
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
                    deleteMarkers()
                    deletePaths()
                    drawMarkers(location)
                    drawPolyline(location)
                    callTaxiViewModel.getCurrentLocation()
                }
            }
        }
        callTaxiViewModel.currentLocation.observe(viewLifecycleOwner) { state ->
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
                    preDistance = distance
                    distance = state.data.dis.toInt()
                    if(distance < 0 && preDistance > 0){
                        arrivalDestination()
                    }else{
                            updateMarker(state.data)
                    }
                }
            }
        }
    }

    private fun updateMarker(location: CurrentLocation){
        binding.textDrivingTaxiAddress.text = getAddress(location.lati, location.long)
        markers[markers.lastIndex].map = null
        markers.removeAt(markers.lastIndex)
        markers.add(createTaxiMarker(Location(location.lati.toString(), location.long.toString())))
        var str = ((location.dis/1000.0) * 100.0).roundToInt() / 100.0
        binding.textDrivingTaxiDistance.text = str.toString() +"Km"
        infoWindow.adapter = rootView?.let {
            MarkerInfoAdapter(requireContext(),
                it, str.toString()+"Km", location.time.toString()+"분")
        }!!
        infoWindow.open(markers[markers.lastIndex])
        naverMap?.moveCamera(
            CameraUpdate.scrollTo(
                LatLng(
                    location.lati,
                    location.long
                )
            )
        )
        binding.textDrivingTaxiFee.text = "1,000 원"
        //getFee(((location.dis/1000.0) * 100.0).roundToInt() / 100.0)
    }

    private fun getAddress(lat: Double, lng: Double): String {
        val geoCoder = Geocoder(requireContext(), Locale.KOREA)
        val address: ArrayList<Address>
        var addressResult = "주소를 가져 올 수 없습니다."
        try {
            //세번째 파라미터는 좌표에 대해 주소를 리턴 받는 갯수로
            //한좌표에 대해 두개이상의 이름이 존재할수있기에 주소배열을 리턴받기 위해 최대갯수 설정
            address = geoCoder.getFromLocation(lat, lng, 1) as ArrayList<Address>
            if (address.size > 0) {
                // 주소 받아오기
                val currentLocationAddress = address[0].getAddressLine(0)
                    .toString()
                addressResult = currentLocationAddress

            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return addressResult
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
            markers.add(createMarker(location[location.lastIndex]))
            markers.add(createTaxiMarker(location[0]))
            infoWindow.adapter = rootView?.let {
                MarkerInfoAdapter(requireContext(),
                    it, "거리", "비용")
            }!!
            infoWindow.open(markers[markers.lastIndex])

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

    private fun createMarker(location: Location): Marker {
        return Marker().apply {
            position = LatLng(location.lati.toDouble(), location.long.toDouble())    // 마커 좌표
            icon = OverlayImage.fromResource(R.drawable.ic_marker)
            iconTintColor = requireActivity().getColor(R.color.primaryColor)// 마커 색깔
            width = requireContext().getPxFromDp(48f)   // 마커 가로 크기
            height = requireContext().getPxFromDp(48f)  // 마커 세로 크기
            zIndex = 0  // 마커 높이
            onClickListener = Overlay.OnClickListener {     // 마커 클릭 리스너
                return@OnClickListener true
            }
            isHideCollidedMarkers = true    // 겹치면 다른 마커 숨기기
            map = naverMap  // 지도에 마커 표시
        }
    }

    private fun createTaxiMarker(location: Location): Marker {
        return Marker().apply {
            Glide.with(requireContext())
                .asBitmap()
                .load(ApplicationClass.prefs.carImage)
                .apply(RequestOptions().centerCrop().circleCrop())
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        this.apply {
                            icon = OverlayImage.fromBitmap(
                                Bitmap.createScaledBitmap(
                                    resource,
                                    requireContext().getPxFromDp(64f),
                                    requireContext().getPxFromDp(64f),
                                    true
                                )
                            )
                        }
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
            position = LatLng(location.lati.toDouble(), location.long.toDouble())    // 마커 좌표
            width = requireContext().getPxFromDp(48f)   // 마커 가로 크기
            height = requireContext().getPxFromDp(48f)  // 마커 세로 크기
            zIndex = 0  // 마커 높이
            isHideCollidedMarkers = true    // 겹치면 다른 마커 숨기기
            map = naverMap
        }
    }

    private fun drawPolyline(location: List<Location>) {
        if (location.isNotEmpty()) {
            if (location.size >= 2) {  // 한 여행에서 두 개 이상 여행지 방문했을 때만 경로 그리기
                paths.add(PathOverlay().apply {
                    color = requireActivity().getColor(R.color.primaryColor) // 경로 색깔
                    outlineColor = requireActivity().getColor(R.color.primaryColor) // 경로 색깔
                    outlineWidth = requireContext().getPxFromDp(1.5f) // 경로 두께
                    var list = mutableListOf<LatLng>()
                    for(i in location){
                        list.add(LatLng(i.lati.toDouble(), i.long.toDouble()))
                    }
                    coords = list  // 경로 좌표
                    map = naverMap
                })
            }
        }
    }

    private fun initNaverMap() {
        val _naverMap =
            childFragmentManager.findFragmentById(R.id.fragmentContainer_driving_taxi) as MapFragment?
                ?: MapFragment.newInstance().also {
                    childFragmentManager.beginTransaction()
                        .add(R.id.fragmentContainer_driving_taxi, it)
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
            isLiteModeEnabled = false // 가벼운 지도 모드 (건물 내부 상세 표시 X)

            this@DrivingTaxiFragment.uiSettings = this.uiSettings.apply {
                isCompassEnabled = false // 나침반 비활성화
                isZoomControlEnabled = false // 확대 축소 버튼 비활성화
                isScaleBarEnabled = false // 스케일 바 비활성화
                isLocationButtonEnabled = false // 기본 내 위치 버튼 비활성화
            }
        }
    }

    private fun getRoute() {
        callTaxiViewModel.addRouteSetting(
            RouteSetting(destination = Location(
                lati = ApplicationClass.prefs.destinationLatitude.toString(), long = ApplicationClass.prefs.destinationLongitude.toString()),
                startingPoint = Location(lati = "0", long = "0"),
                checkState = true
            )
        )
    }

    private fun getFee(distance: Double){
        var distance = distance
        val numberFormat: NumberFormat = NumberFormat.getInstance()
        if(distance <= 3){
            var feeNum = numberFormat.format(3000)
            binding.textDrivingTaxiFee.text = feeNum + " 원"
        }else{
            distance -= 3
            var res = distance/0.16
            fee = 3000 + (res.toInt()*100)
            var feeNum = numberFormat.format(fee)
            binding.textDrivingTaxiFee.text = feeNum + " 원"
        }
    }

    private fun arrivalDestination(){
        ApplicationClass.prefs.fee = fee
        findNavController().navigate(R.id.action_drivingTaxiFragment_to_endDrivingTaxiFragment)
    }

}