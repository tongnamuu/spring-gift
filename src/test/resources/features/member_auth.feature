# language: ko
@blackbox @api @member
기능: 회원 가입과 로그인
  사용자는 이메일과 비밀번호로 가입하고 로그인하여 인증 토큰을 받을 수 있다.

  시나리오: 새 회원 가입에 성공한다
    만약 사용자가 "new-user@example.com" 이메일과 "password123" 비밀번호로 회원 가입을 요청하면
    그러면 응답 상태는 201이다
    그리고 응답 본문에 인증 토큰이 포함된다

  시나리오: 이미 등록된 이메일로는 가입할 수 없다
    먼저 "duplicate@example.com" 이메일과 "password123" 비밀번호로 가입한 회원이 있다
    만약 사용자가 "duplicate@example.com" 이메일과 "password123" 비밀번호로 회원 가입을 요청하면
    그러면 응답 상태는 400이다

  시나리오: 등록된 회원은 로그인할 수 있다
    먼저 "login-user@example.com" 이메일과 "password123" 비밀번호로 가입한 회원이 있다
    만약 사용자가 "login-user@example.com" 이메일과 "password123" 비밀번호로 로그인을 요청하면
    그러면 응답 상태는 200이다
    그리고 응답 본문에 인증 토큰이 포함된다

  시나리오: 비밀번호가 틀리면 로그인할 수 없다
    먼저 "wrong-password@example.com" 이메일과 "password123" 비밀번호로 가입한 회원이 있다
    만약 사용자가 "wrong-password@example.com" 이메일과 "wrong-password" 비밀번호로 로그인을 요청하면
    그러면 응답 상태는 400이다
