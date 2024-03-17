INSERT
INTO
  users
  (id, full_name, email, password, avatar_url, location, telegram_id, role)
VALUES
  ('631bde5d-a850-4539-9f40-053c41ef07b1', 'Peter Berger',  'peter.berger@testmail.com',  '$2a$10$cFNxO/yS0zBfPhtnFQ779Od3LiMoqSG0zrh7.Pb98onbi4Oajwwqy', null, 'Lviv',     null, 'ROLE_USER'),
  ('631bde5d-a850-4539-9f40-053c41ef07b2', 'Laura Splice',  'laura.splice@testmail.com',  '$2a$10$cFNxO/yS0zBfPhtnFQ779Od3LiMoqSG0zrh7.Pb98onbi4Oajwwqy', null, 'Dnipro',   null, 'ROLE_USER'),
  ('631bde5d-a850-4539-9f40-053c41ef07b3', 'Amiya Shiro',   'amiya.shiro@testmail.com',   '$2a$10$cFNxO/yS0zBfPhtnFQ779Od3LiMoqSG0zrh7.Pb98onbi4Oajwwqy', null, 'Kharkiv',  null, 'ROLE_USER'),
  ('631bde5d-a850-4539-9f40-053c41ef07b4', 'Saria Darkoff', 'saria.darkoff@testmail.com', '$2a$10$cFNxO/yS0zBfPhtnFQ779Od3LiMoqSG0zrh7.Pb98onbi4Oajwwqy', null, 'Kyiv',     null, 'ROLE_USER');

INSERT
INTO
  books
  (id, owner_id, title, author, genre, description, image_name, image_url, language, publication_date, trade_format)
VALUES
  ('d4cd1155-993b-4e42-818f-a963f7e4fea1', '631bde5d-a850-4539-9f40-053c41ef07b1', 'The Great Gatsby',                                  'F.Scott Fitzgerald',          'fiction', 'Description...', null, null, 'ENGLISH', 2024, 'GIFT'),
  ('d4cd1155-993b-4e42-818f-a963f7e4fea2', '631bde5d-a850-4539-9f40-053c41ef07b2', 'Golden Fool',                                       'Robin Hobb',                  'fiction', 'Description...', null, null, 'ENGLISH', 2024, 'TRADE'),
  ('d4cd1155-993b-4e42-818f-a963f7e4fea3', '631bde5d-a850-4539-9f40-053c41ef07b2', 'Seven Blades in Black',                             'Sam Sykes',                   'fiction', 'Description...', null, null, 'ENGLISH', 2024, 'TRADE'),
  ('d4cd1155-993b-4e42-818f-a963f7e4fea4', '631bde5d-a850-4539-9f40-053c41ef07b2', 'One Hundred Years of Solitude',                     'Gabriel García Márquez',      'fiction', 'Description...', null, null, 'ENGLISH', 2024, 'TRADE'),
  ('d4cd1155-993b-4e42-818f-a963f7e4fea5', '631bde5d-a850-4539-9f40-053c41ef07b3', 'Love in the Time of Cholera',                       'Gabriel García Márquez',      'fiction', 'Description...', null, null, 'ENGLISH', 2024, 'GIFT'),
  ('d4cd1155-993b-4e42-818f-a963f7e4fea6', '631bde5d-a850-4539-9f40-053c41ef07b3', 'American Prometheus: The Inspiration',              'Kai Bird, Martin J. Sherwin', 'fiction', 'Description...', null, null, 'ENGLISH', 2024, 'GIFT'),
  ('d4cd1155-993b-4e42-818f-a963f7e4fea7', '631bde5d-a850-4539-9f40-053c41ef07b3', 'The 48 Lows of Power',                              'Robert Greene',               'fiction', 'Description...', null, null, 'ENGLISH', 2024, 'TRADE'),
  ('d4cd1155-993b-4e42-818f-a963f7e4fea8', '631bde5d-a850-4539-9f40-053c41ef07b4', 'Can`t Hurt Me: Master Your Mind and Defy the Odds', 'David Goggins',               'fiction', 'Description...', null, null, 'ENGLISH', 2024, 'TRADE'),
  ('d4cd1155-993b-4e42-818f-a963f7e4fea9', '631bde5d-a850-4539-9f40-053c41ef07b4', 'The Woman in Me',                                   'Britney Spears',              'fiction', 'Description...', null, null, 'ENGLISH', 2024, 'GIFT'),
  ('d4cd1155-993b-4e42-818f-a963f7e4fe7d', '631bde5d-a850-4539-9f40-053c41ef07b4', 'Jackie: Public, Private, Secret',                   'J, Randy Taraborrelli',       'fiction', 'Description...', null, null, 'ENGLISH', 2024, 'TRADE');
