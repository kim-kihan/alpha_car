#!/usr/bin/env python
# -*- coding: utf-8 -*-

import rospy
import rospkg
import sys
import os
import heapq
import json
import threading

from morai_msgs.msg import GPSMessage
from geometry_msgs.msg import Point32,PoseStamped
from nav_msgs.msg import Odometry,Path

current_path = os.path.dirname(os.path.realpath(__file__))
sys.path.append(current_path)

from lib.mgeo.class_defs import *
from pyproj import Proj

# firestore 접근권한 획득
import firebase_admin
from firebase_admin import credentials, firestore
cred = credentials.Certificate("/home/ssafy/catkin_ws/src/serviceAccountkey.json")
firebase_admin.initialize_app(cred)

# firestore 연결
db = firestore.client()
doc_ref = db.collection(u'RouteSetting').document(u'12가3456')





class Basic_firebase:
    def __init__(self):
        rospy.init_node('Dij', anonymous=True)
        rospy.Subscriber("/odom", Odometry, self.callback3)


        # ROS에 전달할 global_path 
        self.global_path_pub = rospy.Publisher('/global_path',Path, queue_size = 1)
        self.global_path_msg = Path()
        self.global_path_msg.header.frame_id = 'map'


        # 받아올 상암 맵
        load_path = os.path.normpath(os.path.join(current_path, '../../Sangam_Mgeo'))
        mgeo_planner_map = MGeo.create_instance_from_json(load_path)

        self.node_set = mgeo_planner_map.node_set
        self.link_set = mgeo_planner_map.link_set

        self.nodes = self.node_set.nodes
        self.links = self.link_set.lines


        # 좌표변환용 (GPS -> 맵 좌표계)
        self.proj_UTM = Proj(proj='utm',zone=52, ellps='WGS84', preserve_units=False)
        self.e_o, self.n_o = 313008.55819800857, 4161698.628368007

        # 가까운 노드 찾기
        file_path = "/home/ssafy/catkin_ws/src/nodelist.json"
        with open(file_path, "r") as json_file:
            self.json_data = json.load(json_file)

        file_path2 = "/home/ssafy/catkin_ws/src/newnew.json"
        with open(file_path2, "r") as json_file2:
            self.json_data2 = json.load(json_file2)


        # 다익스트라 비용 테이블
        self.weight_table = self.dijkstra_weight_table()
        #############################################################################

        prev = {
            'checkState' : True,
            'destination' : {
                'lati' : "0", 'long' : "0"
            },
            'startingPoint' : {
                'lati' : "0", 'long' : "0"
            }
        }
        self.newData = {
            'checkState' : True,
            'destination' : {
                'lati' : "0", 'long' : "0"
            },
            'startingPoint' : {
                'lati' : "0", 'long' : "0"
            }
        }


        # Create an Event for notifying main thread.
        callback_done = threading.Event()

        # Create a callback on_snapshot function to capture changes
        def on_snapshot(doc_snapshot, changes, read_time):
            for change in changes:
                if change.type.name == 'MODIFIED':
                    doc_ref = db.collection(u'RouteSetting').get()[0].to_dict()
                    self.newData = {
                        'checkState' : doc_ref[u'checkState'],
                        'destination' : {
                            'lati' : doc_ref[u'destination']['lati'], 'long' : doc_ref[u'destination']['long']
                        },
                        'startingPoint' : {
                            'lati' : doc_ref[u'startingPoint']['lati'], 'long' : doc_ref[u'startingPoint']['long']
                        }
                    }
                    break
            callback_done.set()

        doc_watch = doc_ref.on_snapshot(on_snapshot)

        self.on_path = None
        self.x = None
        self.y = None
        self.is_me = None

        

        rate = rospy.Rate(1) # 1hz
        while not rospy.is_shutdown():
            # print(self.on_path != None, self.is_me)
            if self.newData['checkState'] == True and self.on_path != None and self.is_me != None:
                    if len(self.on_path) > 4:
                    
                        start_x, start_y, _ = self.on_path[1]
                        end_x, end_y, _ = self.on_path[-2]

                        total_line_dis = (start_x - end_x)**2 + (start_y - end_y)**2
                        line_dis = (self.x - end_x)**2 + (self.y - end_y)**2
                        res_distance = len(self.on_path) * 1.3
                        
                        x = line_dis * res_distance / total_line_dis
                        answer = x

                        data333 = {
                                'dis' : -1 if 0 < answer < 5 else answer,
                                'time' : answer / 30000 * 60
                            }
                        db.collection(u'CurrentLocation').document(u'Ego_0').update(data333)
            # if self.newData['checkState'] == True and self.on_path != None:
            #     doc_ref2 = db.collection(u'CurrentLocation').get()[0].to_dict()
            #     start_x = doc_ref2['long']
            #     start_y = doc_ref2['lati']
            #     des_x = float(self.newData['destination']['long'])
            #     des_y = float(self.newData['destination']['lati'])
                
            #     startNode = self.find_shortest_node(start_x, start_y)
            #     endNode = self.find_shortest_node(des_x, des_y)
                
            #     if startNode in self.on_path:
            #         print("find right node to")
            #         _, now_points = self.dijkstra(startNode, endNode)

            #         res_distance = len(now_points) * 1.3
            #         print(len(now_points) * 1.3, res_distance / 30000 * 60, '||||||')
            #         data333 = {
            #             'dis' : -1 if 0 < len(now_points)< 10 else res_distance,
            #             'time' : res_distance / 30000 * 60
            #             }
            #         db.collection(u'CurrentLocation').document(u'Ego_0').update(data333)


            if prev != self.newData:
                newData = self.newData
                print("prev != newData !!!!!!!!!!!!!!!!!!!!!!!")
                # 1. 좌표변환
                start_x = float(newData['startingPoint']['long'])
                start_y = float(newData['startingPoint']['lati'])
                des_x = float(newData['destination']['long'])
                des_y = float(newData['destination']['lati'])

                if newData['startingPoint']['long'] == "0" and newData['startingPoint']['lati'] == "0":
                    doc_ref2 = db.collection(u'CurrentLocation').get()[0].to_dict()
                    start_x = doc_ref2['long']
                    start_y = doc_ref2['lati']
                    print('call taxi !!!!!!!!!!!!!!!!!!!!!!!!')

                # 2. 가까운 노드 찾기
                startNode = self.find_shortest_node(start_x, start_y)
                endNode = self.find_shortest_node(des_x, des_y)
                print(startNode, endNode)

                # 3. 다익스트라 실행
                link_idx_array, points_list = self.dijkstra(startNode, endNode)
                self.on_path = points_list
                
                # 4-1. FB에 경로 저장
                answer = []
                for link_idx in link_idx_array:
                    if link_idx == -1:
                        continue
                    answer.append(self.json_data2[link_idx][0])

                if link_idx_array and link_idx_array[-1] != -1 and len(self.json_data2[link_idx_array[-1]]) > 1:
                    answer.append(self.json_data2[link_idx_array[-1]][1])

                print("mobile_path", answer)
                print(len(points_list) * 1.3)
                data22 = {
                    'route' : answer
                }

                
                db.collection(u'Route').document(u'12가3456').set(data22)
                

                ###################
                for point in points_list:
                    read_pose = PoseStamped()
                    read_pose.pose.position.x = float(point[0])
                    read_pose.pose.position.y = float(point[1])
                    if float(point[2]) == -1:
                        read_pose.pose.orientation.w = -1
                    else:
                        read_pose.pose.orientation.w = 1

                    self.global_path_msg.poses.append(read_pose)



                # 4-2. start driving
                if newData['checkState'] == True:
                    # global path publish
                    self.global_path_pub.publish(self.global_path_msg)
                else:
                    data333 = {'dis' : len(points_list)* 1.3}
                    db.collection(u'Distance').document(u'12가3456').set(data333)
                prev = newData
            rate.sleep()


    def callback3(self, msg):
        self.x = msg.pose.pose.position.x
        self.y = msg.pose.pose.position.y
        self.is_me = True

    def find_shortest_node(self, long, lati):
        shortest_node = None
        distance = float('inf')
        for node_idx, data in self.json_data.items():
            temp = (long - data[0])**2 + (lati - data[1])**2
            if temp < distance:
                distance = temp
                shortest_node = node_idx

        return shortest_node


    def dijkstra_weight_table(self):
        # 초기 설정 : 링크길이 기준으로 비용 테이블 생성
        weight = dict()

        # 초기화
        for from_node_id, from_node in self.nodes.items():
            weight_from_this_node = dict()

            changeable_nodes = []
            for next_link in from_node.get_to_links():
                if next_link.get_all_left_links():
                    for left_link in next_link.get_all_left_links():
                        changeable_nodes.append(left_link.to_node)
                        
                if next_link.get_all_right_links():
                    for right_link in next_link.get_all_right_links():
                        changeable_nodes.append(right_link.to_node)


            for node in from_node.get_to_nodes() + changeable_nodes:
                weight_from_this_node[node.idx] = float('inf')
            weight[from_node_id] = weight_from_this_node
        

        for from_node_idx in weight.keys():
            first_link_cost = 0
            for next_node_idx in weight[from_node_idx].keys():
                shortest_link = None
                min_cost = float('inf')

                for key, link in self.links.items():
                    if link.from_node.idx == from_node_idx and link.to_node.idx == next_node_idx:
                        min_cost = len(link.points)
                        shortest_link = link.idx
                        if not first_link_cost:
                            first_link_cost = len(link.points) + 1
                        break
                else:
                    shortest_link = -1
                    min_cost = first_link_cost

                weight[from_node_idx][next_node_idx] = [min_cost, shortest_link]

        return weight


    def dijkstra(self, startNode, endNode):
        shortest_ways = {}
        distances = {}
        for node_id in self.weight_table.keys():
            shortest_ways[node_id] = []
            distances[node_id] = float('inf')

        # 시작지점은 비용 0
        distances[startNode] = 0
        queue = []

        heapq.heappush( queue, [distances[startNode], startNode] )
        while queue:
            distance, start_idx = heapq.heappop(queue)
            if distances[start_idx] < distance:
                continue

            for next_node in self.weight_table[start_idx]:
                contents = self.weight_table[start_idx][next_node]
                if distance + contents[0] < distances[next_node]:
                    distances[next_node] = distance + contents[0]
                    shortest_ways[next_node] = shortest_ways[start_idx] + [contents[1]]
                    
                    heapq.heappush(queue, [distances[next_node], next_node])


        point_path = []        
        for link_id in shortest_ways[endNode]:
            if link_id == -1:
                point_path.append([-1, -1, -1])
            else:
                link = self.links[link_id]

                for point in link.points:
                    point_path.append([point[0], point[1], 0])


        # 모바일용 노드리스트, ROS용 포인트path
        return shortest_ways[endNode], point_path


if __name__ == '__main__':
    try:
        Basic_firebase()
    except rospy.ROSInterruptException:
        pass