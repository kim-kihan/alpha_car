package com.example.taxi.ui.call_taxi.location

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
import com.example.taxi.databinding.FragmentLocationTrackingTaxiBinding
import com.example.taxi.di.ApplicationClass
import com.example.taxi.ui.call_taxi.CallTaxiViewModel
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
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


@AndroidEntryPoint
class LocationTrackingTaxiFragment : BaseFragment<FragmentLocationTrackingTaxiBinding>(R.layout.fragment_location_tracking_taxi),
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
        setOnClickListeners()
    }

    private fun initView(){
        binding.textLocationTrackingTaxiName.text = ApplicationClass.prefs.carName
        Glide.with(requireContext())
            .load(ApplicationClass.prefs.carImage)
            .into(binding.imageLocationTrackingTaxiCar)
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
                    if(markers.size > 0){
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
    }

    private fun updateMarker(location: CurrentLocation){
        binding.textLocationTrackingTaxiAddress.text = getAddress(location.lati, location.long)
        markers[markers.lastIndex].map = null
        markers.removeAt(markers.lastIndex)
        markers.add(createTaxiMarker(Location(location.lati.toString(), location.long.toString())))
        var str = ((location.dis/1000.0) * 100.0).roundToInt() / 100.0
        infoWindow.adapter = rootView?.let {
            MarkerInfoAdapter(requireContext(),
                it, str.toString()+"Km", "???"+location.time.toInt().toString()+"???")
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

    }

    private fun getAddress(lat: Double, lng: Double): String {
        val geoCoder = Geocoder(requireContext(), Locale.KOREA)
        val address: ArrayList<Address>
        var addressResult = "????????? ?????? ??? ??? ????????????."
        try {
            //????????? ??????????????? ????????? ?????? ????????? ?????? ?????? ?????????
            //???????????? ?????? ??????????????? ????????? ????????????????????? ??????????????? ???????????? ?????? ???????????? ??????
            address = geoCoder.getFromLocation(lat, lng, 1) as ArrayList<Address>
            if (address.size > 0) {
                // ?????? ????????????
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
                    it, "??????", "??????")
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
            position = LatLng(location.lati.toDouble(), location.long.toDouble())    // ?????? ??????
            icon = OverlayImage.fromResource(R.drawable.ic_marker)
            iconTintColor = requireActivity().getColor(R.color.primaryColor)// ?????? ??????
            width = requireContext().getPxFromDp(48f)   // ?????? ?????? ??????
            height = requireContext().getPxFromDp(48f)  // ?????? ?????? ??????
            zIndex = 0  // ?????? ??????
            onClickListener = Overlay.OnClickListener {     // ?????? ?????? ?????????
                return@OnClickListener true
            }
            isHideCollidedMarkers = true    // ????????? ?????? ?????? ?????????
            map = naverMap  // ????????? ?????? ??????
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
            position = LatLng(location.lati.toDouble(), location.long.toDouble())    // ?????? ??????
            width = requireContext().getPxFromDp(48f)   // ?????? ?????? ??????
            height = requireContext().getPxFromDp(48f)  // ?????? ?????? ??????
            zIndex = 0  // ?????? ??????
            isHideCollidedMarkers = true    // ????????? ?????? ?????? ?????????
            map = naverMap
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
        binding.imgLocationTrackingTaxiCall.setOnClickListener {
            val tt: Intent =
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:01077777777"))
            startActivity(tt)
        }
    }

    private fun initNaverMap() {
        val _naverMap =
            childFragmentManager.findFragmentById(R.id.fragmentContainer_location_tracking_taxi) as MapFragment?
                ?: MapFragment.newInstance().also {
                    childFragmentManager.beginTransaction()
                        .add(R.id.fragmentContainer_location_tracking_taxi, it)
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

            this@LocationTrackingTaxiFragment.uiSettings = this.uiSettings.apply {
                isCompassEnabled = false // ????????? ????????????
                isZoomControlEnabled = false // ?????? ?????? ?????? ????????????
                isScaleBarEnabled = false // ????????? ??? ????????????
                isLocationButtonEnabled = false // ?????? ??? ?????? ?????? ????????????
            }
        }
    }

    private fun getRoute() {
        callTaxiViewModel.addRouteSetting(
            RouteSetting(destination = Location(
                lati = ApplicationClass.prefs.startLatitude.toString(), long = ApplicationClass.prefs.startLongitude.toString()),
                startingPoint = Location(lati = "0", long = "0"),
                checkState = true
            )
        )
    }

    private fun arrivalDestination(){
        findNavController().navigate(R.id.action_locationTrackingTaxiFragment_to_startDrivingTaxiFragment)
    }

}