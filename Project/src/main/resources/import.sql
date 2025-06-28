INSERT INTO MEMBERS         (username, is_admin, is_connected) VALUES ('u1', TRUE,  FALSE);
INSERT INTO MEMBERS         (username, is_admin, is_connected) VALUES ('u2', FALSE, TRUE);
INSERT INTO MEMBERS         (username, is_admin, is_connected) VALUES ('u3', FALSE, FALSE);
INSERT INTO MEMBERS         (username, is_admin, is_connected) VALUES ('u4', FALSE, FALSE);
INSERT INTO MEMBERS         (username, is_admin, is_connected) VALUES ('u5', FALSE, FALSE);
INSERT INTO MEMBERS         (username, is_admin, is_connected) VALUES ('u6', FALSE, FALSE);

INSERT INTO AUTH_TOKENS      (token, expiration_time, user_id)  VALUES ('token-u2', DATEADD('HOUR',1,CURRENT_TIMESTAMP), 2);

INSERT INTO SHOPS            (name, is_closed)                  VALUES ('s1', FALSE);

INSERT INTO ITEMS            (name)                             VALUES ('Bamba');
INSERT INTO SHOP_ITEMS       (shop_id, item_id, quantity)       VALUES (1, 1, 20);
INSERT INTO SHOP_ITEM_PRICES (shop_id, item_id, price)          VALUES (1, 1, 30);

INSERT INTO MEMBER_ROLES     (member_id, assignee_id, permissions, shop_id) VALUES (2, 3, ARRAY[1], 1);
INSERT INTO MEMBER_ROLES     (member_id, assignee_id, permissions, shop_id) VALUES (2, 4, ARRAY[8], 1);
INSERT INTO MEMBER_ROLES     (member_id, assignee_id, permissions, shop_id) VALUES (2, 5, ARRAY[8], 1);

UPDATE MEMBERS               SET is_connected = FALSE WHERE member_id = 2;
