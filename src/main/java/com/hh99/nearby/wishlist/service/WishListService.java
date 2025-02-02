package com.hh99.nearby.wishlist.service;

import com.hh99.nearby.entity.Challenge;
import com.hh99.nearby.entity.Member;
import com.hh99.nearby.entity.QWishList;
import com.hh99.nearby.entity.WishList;
import com.hh99.nearby.exception.PrivateException;
import com.hh99.nearby.exception.ErrorCode;
import com.hh99.nearby.repository.ChallengeRepository;
import com.hh99.nearby.repository.MemberRepository;
import com.hh99.nearby.repository.WishListRepository;
import com.hh99.nearby.wishlist.dto.MypageWishList;
import com.hh99.nearby.wishlist.dto.MypageWishResponseDto;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WishListService {
    private final WishListRepository wishListRepository;
    private final MemberRepository memberRepository;
    private final ChallengeRepository challengeRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Transactional
    public ResponseEntity<?> createWishList(@PathVariable Long id, UserDetails user){
        Optional<Challenge> challenge = challengeRepository.findById(id);
        if (challenge.isEmpty()){
            throw new PrivateException(ErrorCode.WISH_CHALLENGE_NOTFOUND);
        }
        Optional<Member> member = memberRepository.findByNickname(user.getUsername());
        if (member.isEmpty()){
            throw new PrivateException(ErrorCode.WISH_MEMBER_NOTFOUND);
        }
        Optional<WishList> optionalWishList =
                wishListRepository.findByChallengeAndMember(challenge.get(),member.get());
        if (optionalWishList.isPresent()){
            throw new PrivateException(ErrorCode.WISH_ALREADY_SELECT);
        }

        WishList wishList = WishList.builder()
                .challenge(challenge.get())
                .member(member.get())
                .build();
        wishListRepository.save(wishList);
        return ResponseEntity.ok().body(Map.of("msg","찜 등록을 완료했습니다."));
    }

    @Transactional
    public ResponseEntity<?> deleteWishList(@PathVariable Long id,UserDetails user){

        Optional<Challenge> challenge = challengeRepository.findById(id);
        if (challenge.isEmpty()){
            throw new PrivateException(ErrorCode.WISH_CHALLENGE_NOTFOUND);
        }
        Optional<Member> member = memberRepository.findByNickname(user.getUsername());
        if (member.isEmpty()){
            throw new PrivateException(ErrorCode.WISH_MEMBER_NOTFOUND);
        }
        Optional<WishList> wishList =
                wishListRepository.findByChallengeAndMember(challenge.get(),member.get());
        if (wishList.isEmpty()){
            throw new PrivateException(ErrorCode.WISH_NOT_SELECT);
        }
        if (!user.getUsername().equals(wishList.get().getMember().getNickname())){
            throw new PrivateException(ErrorCode.WISH_DELETE_FORBIDDEN);
        }
        wishListRepository.delete(wishList.get());
        return ResponseEntity.ok().body(Map.of("msg","찜 등록을 취소하셨습니다."));
    }

    public ResponseEntity<?> getWishList(UserDetails user,int pageNum){
        Optional<Member> member = memberRepository.findByNickname(user.getUsername());
        pageNum = pageNum -1;
        int size = 4;
        Pageable pageable = PageRequest.of(pageNum,size);
        List<WishList> wishLists = getWishLists(member.get(),pageable);
        List<WishList> wishListSize = getWishLists(member.get());
        List<MypageWishList> mypageWishList = new ArrayList<>();
        for (int i = 0; i<wishLists.size(); i++){
            long participatePeople = wishLists.get(i).getChallenge().getMemberChallengeList().size();
            mypageWishList.add(
                    MypageWishList.builder()
                            .title(wishLists.get(i).getChallenge().getTitle())
                            .challengeImg(wishLists.get(i).getChallenge().getChallengeImg())
                            .startDay(wishLists.get(i).getChallenge().getStartDay())
                            .startTime(wishLists.get(i).getChallenge().getStartTime())
                            .tagetTime(wishLists.get(i).getChallenge().getTargetTime())
                            .endTime(wishLists.get(i).getChallenge().getEndTime())
                            .limitPeople(wishLists.get(i).getChallenge().getLimitPeople())
                            .participatePeople(participatePeople)
                            .build()
            );

        }
        double totalPage = Math.ceil((double)wishListSize.size()/(double) size);
        MypageWishResponseDto mypageWishResponseDto = MypageWishResponseDto.builder()
                .totalPage((int)totalPage)
                .mypageWishList(mypageWishList)
                .build();
        return ResponseEntity.ok().body(Map.of("msg","찜 등록 조회 완료","data",mypageWishResponseDto));
    }

    public List<WishList> getWishLists(Member member,Pageable pageable){
        QWishList wishList = QWishList.wishList;
        return jpaQueryFactory
                .selectFrom(wishList)
                .where(
                        wishList.member.eq(member)
                )
                .orderBy(wishList.challenge.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    public List<WishList> getWishLists(Member member){
        QWishList wishList = QWishList.wishList;
        return jpaQueryFactory
                .selectFrom(wishList)
                .where(
                        wishList.member.eq(member)
                )
                .orderBy(wishList.challenge.id.desc())
                .fetch();
    }
}
