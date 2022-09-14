// Generated by gencpp from file morai_msgs/NpcGhostCmd.msg
// DO NOT EDIT!


#ifndef MORAI_MSGS_MESSAGE_NPCGHOSTCMD_H
#define MORAI_MSGS_MESSAGE_NPCGHOSTCMD_H


#include <string>
#include <vector>
#include <map>

#include <ros/types.h>
#include <ros/serialization.h>
#include <ros/builtin_message_traits.h>
#include <ros/message_operations.h>

#include <std_msgs/Header.h>
#include <morai_msgs/NpcGhostInfo.h>

namespace morai_msgs
{
template <class ContainerAllocator>
struct NpcGhostCmd_
{
  typedef NpcGhostCmd_<ContainerAllocator> Type;

  NpcGhostCmd_()
    : header()
    , npc_list()  {
    }
  NpcGhostCmd_(const ContainerAllocator& _alloc)
    : header(_alloc)
    , npc_list(_alloc)  {
  (void)_alloc;
    }



   typedef  ::std_msgs::Header_<ContainerAllocator>  _header_type;
  _header_type header;

   typedef std::vector< ::morai_msgs::NpcGhostInfo_<ContainerAllocator> , typename ContainerAllocator::template rebind< ::morai_msgs::NpcGhostInfo_<ContainerAllocator> >::other >  _npc_list_type;
  _npc_list_type npc_list;





  typedef boost::shared_ptr< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> > Ptr;
  typedef boost::shared_ptr< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> const> ConstPtr;

}; // struct NpcGhostCmd_

typedef ::morai_msgs::NpcGhostCmd_<std::allocator<void> > NpcGhostCmd;

typedef boost::shared_ptr< ::morai_msgs::NpcGhostCmd > NpcGhostCmdPtr;
typedef boost::shared_ptr< ::morai_msgs::NpcGhostCmd const> NpcGhostCmdConstPtr;

// constants requiring out of line definition



template<typename ContainerAllocator>
std::ostream& operator<<(std::ostream& s, const ::morai_msgs::NpcGhostCmd_<ContainerAllocator> & v)
{
ros::message_operations::Printer< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> >::stream(s, "", v);
return s;
}


template<typename ContainerAllocator1, typename ContainerAllocator2>
bool operator==(const ::morai_msgs::NpcGhostCmd_<ContainerAllocator1> & lhs, const ::morai_msgs::NpcGhostCmd_<ContainerAllocator2> & rhs)
{
  return lhs.header == rhs.header &&
    lhs.npc_list == rhs.npc_list;
}

template<typename ContainerAllocator1, typename ContainerAllocator2>
bool operator!=(const ::morai_msgs::NpcGhostCmd_<ContainerAllocator1> & lhs, const ::morai_msgs::NpcGhostCmd_<ContainerAllocator2> & rhs)
{
  return !(lhs == rhs);
}


} // namespace morai_msgs

namespace ros
{
namespace message_traits
{





template <class ContainerAllocator>
struct IsFixedSize< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> >
  : FalseType
  { };

template <class ContainerAllocator>
struct IsFixedSize< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> const>
  : FalseType
  { };

template <class ContainerAllocator>
struct IsMessage< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> >
  : TrueType
  { };

template <class ContainerAllocator>
struct IsMessage< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> const>
  : TrueType
  { };

template <class ContainerAllocator>
struct HasHeader< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> >
  : TrueType
  { };

template <class ContainerAllocator>
struct HasHeader< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> const>
  : TrueType
  { };


template<class ContainerAllocator>
struct MD5Sum< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> >
{
  static const char* value()
  {
    return "f46c55a2e9ac85bf40936cf393bdb14d";
  }

  static const char* value(const ::morai_msgs::NpcGhostCmd_<ContainerAllocator>&) { return value(); }
  static const uint64_t static_value1 = 0xf46c55a2e9ac85bfULL;
  static const uint64_t static_value2 = 0x40936cf393bdb14dULL;
};

template<class ContainerAllocator>
struct DataType< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> >
{
  static const char* value()
  {
    return "morai_msgs/NpcGhostCmd";
  }

  static const char* value(const ::morai_msgs::NpcGhostCmd_<ContainerAllocator>&) { return value(); }
};

template<class ContainerAllocator>
struct Definition< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> >
{
  static const char* value()
  {
    return "Header header\n"
"\n"
"NpcGhostInfo[] npc_list\n"
"\n"
"================================================================================\n"
"MSG: std_msgs/Header\n"
"# Standard metadata for higher-level stamped data types.\n"
"# This is generally used to communicate timestamped data \n"
"# in a particular coordinate frame.\n"
"# \n"
"# sequence ID: consecutively increasing ID \n"
"uint32 seq\n"
"#Two-integer timestamp that is expressed as:\n"
"# * stamp.sec: seconds (stamp_secs) since epoch (in Python the variable is called 'secs')\n"
"# * stamp.nsec: nanoseconds since stamp_secs (in Python the variable is called 'nsecs')\n"
"# time-handling sugar is provided by the client library\n"
"time stamp\n"
"#Frame this data is associated with\n"
"string frame_id\n"
"\n"
"================================================================================\n"
"MSG: morai_msgs/NpcGhostInfo\n"
"int32 unique_id\n"
"string name\n"
"\n"
"geometry_msgs/Vector3 position\n"
"geometry_msgs/Vector3 rpy\n"
"\n"
"================================================================================\n"
"MSG: geometry_msgs/Vector3\n"
"# This represents a vector in free space. \n"
"# It is only meant to represent a direction. Therefore, it does not\n"
"# make sense to apply a translation to it (e.g., when applying a \n"
"# generic rigid transformation to a Vector3, tf2 will only apply the\n"
"# rotation). If you want your data to be translatable too, use the\n"
"# geometry_msgs/Point message instead.\n"
"\n"
"float64 x\n"
"float64 y\n"
"float64 z\n"
;
  }

  static const char* value(const ::morai_msgs::NpcGhostCmd_<ContainerAllocator>&) { return value(); }
};

} // namespace message_traits
} // namespace ros

namespace ros
{
namespace serialization
{

  template<class ContainerAllocator> struct Serializer< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> >
  {
    template<typename Stream, typename T> inline static void allInOne(Stream& stream, T m)
    {
      stream.next(m.header);
      stream.next(m.npc_list);
    }

    ROS_DECLARE_ALLINONE_SERIALIZER
  }; // struct NpcGhostCmd_

} // namespace serialization
} // namespace ros

namespace ros
{
namespace message_operations
{

template<class ContainerAllocator>
struct Printer< ::morai_msgs::NpcGhostCmd_<ContainerAllocator> >
{
  template<typename Stream> static void stream(Stream& s, const std::string& indent, const ::morai_msgs::NpcGhostCmd_<ContainerAllocator>& v)
  {
    s << indent << "header: ";
    s << std::endl;
    Printer< ::std_msgs::Header_<ContainerAllocator> >::stream(s, indent + "  ", v.header);
    s << indent << "npc_list[]" << std::endl;
    for (size_t i = 0; i < v.npc_list.size(); ++i)
    {
      s << indent << "  npc_list[" << i << "]: ";
      s << std::endl;
      s << indent;
      Printer< ::morai_msgs::NpcGhostInfo_<ContainerAllocator> >::stream(s, indent + "    ", v.npc_list[i]);
    }
  }
};

} // namespace message_operations
} // namespace ros

#endif // MORAI_MSGS_MESSAGE_NPCGHOSTCMD_H
