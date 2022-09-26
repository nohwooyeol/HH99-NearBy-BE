package com.hh99.nearby.search.service;

import com.hh99.nearby.entity.Challenge;
import com.hh99.nearby.repository.ChallengeRepository;
import com.hh99.nearby.search.dto.SearchResponseDto;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class SearchService {

    private final ChallengeRepository challengeRepository;

    public ResponseEntity<?> search(String keyword,int pageNum) {
        int size = 10;
        pageNum = pageNum-1; //페이지는 0부터 시작하기때문
        Sort.Direction direction = Sort.Direction.DESC; // true: 오름차순 (asc) , 내림차순 DESC(최신 것이 위로온다)
        Sort sort = Sort.by(direction, "id"); // id를 기준으로 내림차순으로 적용
        Pageable pageable = PageRequest.of(pageNum,size,sort); // 페이지 넘버, 글갯수, 정렬
        Page<Challenge> challengeList = challengeRepository. findByTitle(keyword,pageable);
        List<SearchResponseDto> searchResponseDtos = new ArrayList<>();
        for (Challenge challenge : challengeList) {

            long participatePeople = challenge.getMemberChallengeList().size();
            searchResponseDtos.add(SearchResponseDto.builder()
                    .id(challenge.getId())
                    .title(challenge.getTitle())
                    .challengeImg(challenge.getChallengeImg())
                    .startDay(challenge.getStartDay())
                    .startTime(challenge.getStartTime())
                    .tagetTime(challenge.getTargetTime())
                    .endTime(challenge.getEndTime())
                    .limitPeople(challenge.getLimitPeople())
                    .participatePeople(participatePeople)
                    .build());
        }

        return ResponseEntity.ok(searchResponseDtos);
    }
}
