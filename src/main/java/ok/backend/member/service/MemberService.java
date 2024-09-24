package ok.backend.member.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import ok.backend.common.security.util.JwtTokenProvider;
import ok.backend.member.domain.entity.Member;
import ok.backend.member.domain.entity.RefreshToken;
import ok.backend.member.domain.enums.MemberStatus;
import ok.backend.member.domain.repository.MemberRepository;
import ok.backend.member.domain.repository.RefreshTokenRepository;
import ok.backend.member.dto.MemberLoginRequestDto;
import ok.backend.member.dto.MemberRegisterRequestDto;
import ok.backend.member.dto.MemberUpdateRequestDto;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final PasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public Optional<Member> registerMember(MemberRegisterRequestDto memberRegisterRequestDto) {

        String hashPassword = passwordEncoder.encode(memberRegisterRequestDto.getPassword());

        Member member = Member.builder()
                .email(memberRegisterRequestDto.getEmail())
                .password(hashPassword)
                .name(memberRegisterRequestDto.getName())
                .nickname(memberRegisterRequestDto.getNickname())
                .createDate(LocalDate.now())
                .status(MemberStatus.Y)
                .build();

        return Optional.of(memberRepository.save(member));
    }

    public Member findMemberByEmailAndPassword(MemberLoginRequestDto memberLoginRequestDto) {
        Member member = memberRepository.findByEmail(memberLoginRequestDto.getEmail()).orElseThrow(() ->
                new RuntimeException("Member with email " + memberLoginRequestDto.getEmail() + " not found"));

        if(!passwordEncoder.matches(memberLoginRequestDto.getPassword(), member.getPassword())) {
            throw new RuntimeException("Wrong email or password");
        }
        if(member.getStatus() == MemberStatus.N) {
            throw new RuntimeException("탈퇴한 회원입니다.");
        }

        return member;
    }

    @Transactional
    public ResponseCookie createToken(Member member) {

        String accessToken = jwtTokenProvider.createAccessToken(member.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail());

        RefreshToken newRefreshToken = RefreshToken.builder()
                .username(member.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiration(new Date(new Date().getTime() + jwtTokenProvider.getRefreshTokenValidTime()).getTime())
                .build();

        refreshTokenRepository.save(newRefreshToken);

        ResponseCookie cookie = ResponseCookie.from(jwtTokenProvider.getAccessHeader(), accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(3600)
                .build();

        return cookie;
    }

    public void logout(HttpServletRequest request){
        Cookie accessTokenCookie = jwtTokenProvider.resolveAccessToken(request).orElseThrow(() ->
                new RuntimeException("accessToken not found"));
        String accessToken = accessTokenCookie.getValue();

        RefreshToken refreshToken = refreshTokenService.findByAccessToken(accessToken).orElseThrow(() ->
                new RuntimeException("refreshToken not found"));
        refreshTokenService.delete(refreshToken);

        SecurityContextHolder.clearContext();
    }

    @Transactional
    public Member updateMember(MemberUpdateRequestDto memberUpdateRequestDto){
        Member member = memberRepository.findById(memberUpdateRequestDto.getId()).orElseThrow(() ->
                new RuntimeException("Member with id " + memberUpdateRequestDto.getId() + " not found"));

        member.updateMember(memberUpdateRequestDto);
        System.out.println(member.getId());
        memberRepository.save(member);

        return member;
    }

    @Transactional
    public void  deleteMember(Long memberId){
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new RuntimeException("Member with id " + memberId + " not found"));

        member.updateStatus();
        memberRepository.save(member);
    }
}
