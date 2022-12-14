// Generated by gencpp from file ssafy_1/AddTwoInts.msg
// DO NOT EDIT!


#ifndef SSAFY_1_MESSAGE_ADDTWOINTS_H
#define SSAFY_1_MESSAGE_ADDTWOINTS_H

#include <ros/service_traits.h>


#include <ssafy_1/AddTwoIntsRequest.h>
#include <ssafy_1/AddTwoIntsResponse.h>


namespace ssafy_1
{

struct AddTwoInts
{

typedef AddTwoIntsRequest Request;
typedef AddTwoIntsResponse Response;
Request request;
Response response;

typedef Request RequestType;
typedef Response ResponseType;

}; // struct AddTwoInts
} // namespace ssafy_1


namespace ros
{
namespace service_traits
{


template<>
struct MD5Sum< ::ssafy_1::AddTwoInts > {
  static const char* value()
  {
    return "6a2e34150c00229791cc89ff309fff21";
  }

  static const char* value(const ::ssafy_1::AddTwoInts&) { return value(); }
};

template<>
struct DataType< ::ssafy_1::AddTwoInts > {
  static const char* value()
  {
    return "ssafy_1/AddTwoInts";
  }

  static const char* value(const ::ssafy_1::AddTwoInts&) { return value(); }
};


// service_traits::MD5Sum< ::ssafy_1::AddTwoIntsRequest> should match
// service_traits::MD5Sum< ::ssafy_1::AddTwoInts >
template<>
struct MD5Sum< ::ssafy_1::AddTwoIntsRequest>
{
  static const char* value()
  {
    return MD5Sum< ::ssafy_1::AddTwoInts >::value();
  }
  static const char* value(const ::ssafy_1::AddTwoIntsRequest&)
  {
    return value();
  }
};

// service_traits::DataType< ::ssafy_1::AddTwoIntsRequest> should match
// service_traits::DataType< ::ssafy_1::AddTwoInts >
template<>
struct DataType< ::ssafy_1::AddTwoIntsRequest>
{
  static const char* value()
  {
    return DataType< ::ssafy_1::AddTwoInts >::value();
  }
  static const char* value(const ::ssafy_1::AddTwoIntsRequest&)
  {
    return value();
  }
};

// service_traits::MD5Sum< ::ssafy_1::AddTwoIntsResponse> should match
// service_traits::MD5Sum< ::ssafy_1::AddTwoInts >
template<>
struct MD5Sum< ::ssafy_1::AddTwoIntsResponse>
{
  static const char* value()
  {
    return MD5Sum< ::ssafy_1::AddTwoInts >::value();
  }
  static const char* value(const ::ssafy_1::AddTwoIntsResponse&)
  {
    return value();
  }
};

// service_traits::DataType< ::ssafy_1::AddTwoIntsResponse> should match
// service_traits::DataType< ::ssafy_1::AddTwoInts >
template<>
struct DataType< ::ssafy_1::AddTwoIntsResponse>
{
  static const char* value()
  {
    return DataType< ::ssafy_1::AddTwoInts >::value();
  }
  static const char* value(const ::ssafy_1::AddTwoIntsResponse&)
  {
    return value();
  }
};

} // namespace service_traits
} // namespace ros

#endif // SSAFY_1_MESSAGE_ADDTWOINTS_H
