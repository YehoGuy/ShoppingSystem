-- ----------------------------------------------------------------
-- 1) Six users (u1…u6), with u2 initially connected / $2a$10$BQcgCRrZBJZBBzrfoAOxvuBrmSmbfZfUAUgUQr./HbZRzRgqbWPWm = 123
-- ----------------------------------------------------------------
INSERT INTO MEMBERS (
    username, is_admin, is_connected,
    email, password, phone_number,
    suspended, apartment_number, city, country,
    house_number, street, zip_code, payment_method_string
) VALUES
  ('u1', TRUE,  FALSE,
   'u1@example.com', '$2a$10$BQcgCRrZBJZBBzrfoAOxvuBrmSmbfZfUAUgUQr./HbZRzRgqbWPWm', '050-000-0001',
   '2000-01-01 00:00:00', '1A', 'Tel Aviv', 'Israel',
   '10', 'Herzl St', '61000', NULL
  ),
  ('u2', FALSE, TRUE,
   'u2@example.com', '$2a$10$BQcgCRrZBJZBBzrfoAOxvuBrmSmbfZfUAUgUQr./HbZRzRgqbWPWm', '050-000-0002',
   '2000-01-01 00:00:00', '2B', 'Jerusalem', 'Israel',
   '20', 'Ben Yehuda', '91000', NULL
  ),
  ('u3', FALSE, FALSE,
   'u3@example.com', '$2a$10$BQcgCRrZBJZBBzrfoAOxvuBrmSmbfZfUAUgUQr./HbZRzRgqbWPWm', '050-000-0003',
   '2000-01-01 00:00:00', '3C','Haifa', 'Israel',
   '30','HaNassi','33000',NULL
  ),
  ('u4', FALSE, FALSE,
   'u4@example.com', '$2a$10$BQcgCRrZBJZBBzrfoAOxvuBrmSmbfZfUAUgUQr./HbZRzRgqbWPWm', '050-000-0004',
   '2005-05-05 05:05:05', '4D','Beer Sheva','Israel',
   '40','Rothschild','84000',NULL
  ),
  ('u5', FALSE, FALSE,
   'u5@example.com', '$2a$10$BQcgCRrZBJZBBzrfoAOxvuBrmSmbfZfUAUgUQr./HbZRzRgqbWPWm', '050-000-0005',
   '2010-10-10 10:10:10', '5E','Netanya','Israel',
   '50','Allenby','42500',NULL
  ),
  ('u6', FALSE, FALSE,
   'u6@example.com', '$2a$10$BQcgCRrZBJZBBzrfoAOxvuBrmSmbfZfUAUgUQr./HbZRzRgqbWPWm', '050-000-0006',
   '1995-12-31 23:59:59', '6F','Beersheba','Israel',
   '60','Dizengoff','84000',NULL
  )
;

-- ----------------------------------------------------------------
-- 3) Shop “s1”
-- ----------------------------------------------------------------
INSERT INTO SHOPS (name, is_closed, shipping_method_name)
VALUES ('s1', FALSE, 'WSEPShipping');

-- 1) Look up u2’s member_id and s1’s shop_id, then insert one MEMBER_ROLES row
INSERT INTO MEMBER_ROLES (member_id, assignee_id, permissions, shop_id)
SELECT
  auth.member_id,                      -- u2 is granting the role
  auth.member_id,                      -- u2 is also the assignee
  ARRAY[0,1,2,3,5,8,9,10,6,7,11]::smallint[],  -- list of permission‐IDs
  shop.id                              -- the shop “s1”
FROM MEMBERS auth
JOIN SHOPS    shop ON shop.name = 's1'
WHERE auth.username = 'u2';


-- ----------------------------------------------------------------
-- 4) Item “Bamba” in s1, qty=20, price=30
-- ----------------------------------------------------------------
INSERT INTO ITEMS (name, category, description)
VALUES ('Bamba', 1, 'Crunchy peanut snack');

INSERT INTO SHOP_ITEMS (shop_id, item_id, quantity)
SELECT
  s.id,
  i.id,
  20
FROM SHOPS s, ITEMS i
WHERE s.name='s1' AND i.name='Bamba';

INSERT INTO SHOP_ITEM_PRICES (shop_id, item_id, price)
SELECT
  s.id,
  i.id,
  30
FROM SHOPS s, ITEMS i
WHERE s.name='s1' AND i.name='Bamba';


-- -------------------------------------------------------------------
-- Grant roles in shop “s1” by user “u2” to users u3, u4 and u5
--  • u3 should receive:
--      – manageItems            (PermissionsEnum.manageItems → ordinal 0)
--      – leaveShopAsManager     (PermissionsEnum.leaveShopAsManager → ordinal 4)
--  • u4 and u5 should receive:
--      – leaveShopAsManager     (ordinal 4)
-- -------------------------------------------------------------------

INSERT INTO MEMBER_ROLES (member_id, assignee_id, permissions, shop_id)
SELECT
  -- The granter: member_id of u2
  (SELECT member_id
     FROM MEMBERS
    WHERE username = 'u2'
  ) AS member_id,

  -- The target: each of u3, u4, u5
  m.member_id AS assignee_id,

  -- Choose permission arrays:
  --   WHEN u3 → both manageItems (0) and leaveShopAsManager (4)
  --   ELSE    → just leaveShopAsManager (4)
  CASE
    WHEN m.username = 'u3'
      THEN ARRAY[0, 4]::smallint[]
    ELSE
      ARRAY[4]::smallint[]
  END AS permissions,

  -- The shop in which the role applies: “s1”
  (SELECT id
     FROM SHOPS
    WHERE name = 's1'
  ) AS shop_id

FROM MEMBERS m
WHERE m.username IN ('u3', 'u4', 'u5');

-- ----------------------------------------------------------------
-- 6) Finally log u2 out
-- ----------------------------------------------------------------
UPDATE MEMBERS
   SET is_connected = FALSE
 WHERE username = 'u2'
;
