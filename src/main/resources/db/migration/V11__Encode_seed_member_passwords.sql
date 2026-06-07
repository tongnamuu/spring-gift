update member
set password = '$2y$10$MY0SCnICoAGEcs6bJ8iwd.p9IPUzDfjWYcTsybSkd.jPLCXuSW7La'
where email = 'admin@example.com'
  and password = 'admin1234';

update member
set password = '$2y$10$3W3rOyQzx7caJuRaVcCvUuslL8mSvrIJhFz8Pq8gYO4nhdQQGde56'
where email = 'user1@example.com'
  and password = 'password1';

update member
set password = '$2y$10$q6iMGw4dsJgW7pg60Lb30.wNPfO/ZVMVmNAJIGEodotdTX4LdTTW.'
where email = 'user2@example.com'
  and password = 'password2';
