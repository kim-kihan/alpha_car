# 경로계획

자율주행에서 경로계획을 하여 주행 목표를 설정해야 한다.   
이를 위해 사용하는 알고리즘은 다음과 같다.

## Dijkstra 알고리즘

- 그래프에서 노드 간 최단 경로를 찾는 알고리즘
- 다양한 네비게이션 시스템에서 사용되는 기초적인 경로탐색 알고리즘

이미 이전에 정리하였으니 [그것](https://github.com/ii200400/IT_Skill_Question/tree/master/CS/Algorithm#1-dijkstra-algorithm-%EB%8B%A4%EC%9D%B5%EC%8A%A4%ED%8A%B8%EB%9D%BC-%EC%95%8C%EA%B3%A0%EB%A6%AC%EC%A6%98)으로 대체한다.

## Pure pursuit

또 wiki도 없는 내용이다;

- 경로 위의 한 점을 원 호를 그리며 진행하는 방법
- 자율주행 자동차 경로 추종의 대표적 알고리즘
- 자동차의 기구학과 전방주시거리 파라미터만 있다면 간단하게 계산 가능
- 자동차 모델을 단순화한 Bicycle 모델을 사용
    - Bicycle 모델은 뒷 바퀴 1개와 조향이 가능한 앞 바퀴 1개를 가지는 모델

음.. 무슨 의미인지 모르겠으니 결과 값만 뽑아서 사용할 예정이다!