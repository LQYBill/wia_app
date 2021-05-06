INSERT INTO user_client(id, create_by, create_time, update_by,
                        update_time,
                        client_id,
                        user_id)
    VALUE (
           UUID(), 'admin', NOW(), 'admin',
           NOW(),
           (SELECT id FROM client WHERE internal_code = 'EP'),
           (SELECT id FROM sys_user WHERE username = 'test_EP')
    )