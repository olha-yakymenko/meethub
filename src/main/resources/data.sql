INSERT INTO email_templates (template_key, subject, body_template, language) VALUES
('meeting-invitation', 'Zaproszenie do spotkania: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Zostałeś zaproszony do spotkania "{{meetingTitle}}" organizowanego przez {{organizerName}}.<br><br>Data: {{meetingDate}}<br><br><a href="{{confirmationLink}}">Potwierdź udział</a>', 'pl'),
('waitlist-notification', 'Zostałeś dodany do listy oczekujących: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Zostałeś dodany do listy oczekujących na spotkanie "{{meetingTitle}}". Twoja pozycja w kolejce: {{position}}.<br><br>Powiadomimy Cię gdy miejsce się zwolni.', 'pl'),
('waitlist-promotion', 'Miejsce zwolniło się w spotkaniu: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Miejsce zwolniło się w spotkaniu "{{meetingTitle}}" i zostałeś automatycznie zapisany!<br><br>Zapraszamy do udziału.', 'pl')
ON CONFLICT DO NOTHING;

SELECT setval('email_templates_id_seq', COALESCE((SELECT MAX(id) FROM email_templates), 1));

UPDATE email_templates SET
    name = template_key,
    category = 'NOTIFICATION',
    is_active = TRUE,
    version = 1,
    channel = 'EMAIL'
WHERE name IS NULL OR category IS NULL OR is_active IS NULL;

--niepotrzebne
INSERT INTO email_templates (
    template_key,
    subject,
    body_template,
    language,
    name,
    category,
    description,
    variables_help,
    is_active,
    version,
    channel,
    available_variables
) VALUES (
    'participant_joined',
    'Nowy uczestnik w spotkaniu',
    'Użytkownik {{participantName}} dołączył do spotkania "{{meetingTitle}}" dnia {{meetingDate}}.
Aktualna liczba uczestników: {{currentParticipants}}.',
    'pl',
    'Uczestnik dołączył',
    'MEETING',
    'Powiadomienie o nowym uczestniku',
    '{{participantName}}, {{meetingTitle}}, {{meetingDate}}, {{currentParticipants}}',
    true,
    1,
    'EMAIL',
    '{{participantName}},{{meetingTitle}},{{meetingDate}},{{currentParticipants}}'
) ON CONFLICT DO NOTHING;

INSERT INTO email_templates (
    template_key, subject, body_template, language,
    name, category, description, variables_help,
    is_active, version, channel, available_variables
) VALUES (
    'join_request',
    'Nowa prośba o dołączenie do spotkania',
    'Użytkownik {{requesterName}} ({{requesterEmail}}) wysłał prośbę o dołączenie do spotkania "{{meetingTitle}}" zaplanowanego na {{meetingDate}}.',
    'pl',
    'Prośba o dołączenie',
    'MEETING',
    'Powiadomienie o nowej prośbie o dołączenie',
    '{{requesterName}}, {{requesterEmail}}, {{meetingTitle}}, {{meetingDate}}',
    true, 1, 'EMAIL',
    '{{requesterName}},{{requesterEmail}},{{meetingTitle}},{{meetingDate}}'
) ON CONFLICT DO NOTHING;

INSERT INTO email_templates (
    template_key, subject, body_template, language,
    name, category, description, variables_help,
    is_active, version, channel, available_variables
) VALUES (
    'request_approved',
    'Twoja prośba została zaakceptowana',
    'Cześć {{userName}}, Twoja prośba o dołączenie do spotkania "{{meetingTitle}}" w dniu {{meetingDate}} została zaakceptowana przez {{organizerName}}.
Lokalizacja: {{meetingLocation}}.',
    'pl',
    'Prośba zaakceptowana',
    'MEETING',
    'Powiadomienie o zaakceptowaniu prośby',
    '{{userName}}, {{meetingTitle}}, {{meetingDate}}, {{organizerName}}, {{meetingLocation}}',
    true, 1, 'EMAIL',
    '{{userName}},{{meetingTitle}},{{meetingDate}},{{organizerName}},{{meetingLocation}}'
) ON CONFLICT DO NOTHING;

INSERT INTO email_templates (
    template_key, subject, body_template, language,
    name, category, description, variables_help,
    is_active, version, channel, available_variables
) VALUES (
    'request_rejected',
    'Twoja prośba została odrzucona',
    'Cześć {{userName}}, Twoja prośba o dołączenie do spotkania "{{meetingTitle}}" w dniu {{meetingDate}} została odrzucona przez {{organizerName}}.',
    'pl',
    'Prośba odrzucona',
    'MEETING',
    'Powiadomienie o odrzuceniu prośby',
    '{{userName}}, {{meetingTitle}}, {{meetingDate}}, {{organizerName}}',
    true, 1, 'EMAIL',
    '{{userName}},{{meetingTitle}},{{meetingDate}},{{organizerName}}'
) ON CONFLICT DO NOTHING;

INSERT INTO email_templates (template_key, language, subject, body_template, created_at, updated_at)
VALUES
('meeting_started', 'pl',
 '🎉 Spotkanie {{meetingTitle}} się rozpoczęło!',
 '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Spotkanie się rozpoczęło</title>
</head>
<body>
    <h1>🎉 Spotkanie się rozpoczęło!</h1>
    <p>Cześć {{userName}}!</p>
    <p>Twoje spotkanie <strong>{{meetingTitle}}</strong> właśnie się rozpoczęło.</p>
    <p><strong>Czas:</strong> {{meetingTime}}</p>
    {{#location}}<p><strong>Miejsce:</strong> {{location}}</p>{{/location}}
    {{#meetingLink}}<p><strong>Link do spotkania:</strong> <a href="{{meetingLink}}">{{meetingLink}}</a></p>{{/meetingLink}}
    <br>
    <p>Pozdrawiamy,<br>Zespół MeetHub</p>
</body>
</html>',
 NOW(), NOW()),

('meeting_reminder', 'pl',
 '🔔 Przypomnienie: Spotkanie {{meetingTitle}} za {{minutesBefore}} minut',
 '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Przypomnienie o spotkaniu</title>
</head>
<body>
    <h1>🔔 Przypomnienie o spotkaniu</h1>
    <p>Cześć {{userName}}!</p>
    <p>Przypominamy o spotkaniu <strong>{{meetingTitle}}</strong>.</p>
    <p><strong>Rozpoczyna się za:</strong> {{minutesBefore}} minut</p>
    <p><strong>Godzina:</strong> {{meetingTime}}</p>
    {{#location}}<p><strong>Miejsce:</strong> {{location}}</p>{{/location}}
    {{#meetingLink}}<p><strong>Link:</strong> <a href="{{meetingLink}}">{{meetingLink}}</a></p>{{/meetingLink}}
    <br>
    <p>Pozdrawiamy,<br>Zespół MeetHub</p>
</body>
</html>',
 NOW(), NOW()),

('meeting_started_participant', 'pl',
 '🎉 Spotkanie {{meetingTitle}} się rozpoczęło',
 '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Spotkanie się rozpoczęło</title>
</head>
<body>
    <h1>🎉 Spotkanie się rozpoczęło!</h1>
    <p>Cześć {{userName}}!</p>
    <p>Spotkanie <strong>{{meetingTitle}}</strong> właśnie się rozpoczęło.</p>
    <p><strong>Organizator:</strong> {{organizerName}}</p>
    <p><strong>Czas:</strong> {{meetingTime}}</p>
    {{#location}}<p><strong>Miejsce:</strong> {{location}}</p>{{/location}}
    {{#meetingLink}}<p><strong>Link do spotkania:</strong> <a href="{{meetingLink}}">{{meetingLink}}</a></p>{{/meetingLink}}
    <br>
    <p>Do zobaczenia na spotkaniu!<br>Zespół MeetHub</p>
</body>
</html>',
 NOW(), NOW()),

('meeting_reminder_participant', 'pl',
 '🔔 Przypomnienie: Spotkanie {{meetingTitle}} za {{minutesBefore}} minut',
 '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Przypomnienie o spotkaniu</title>
</head>
<body>
    <h1>🔔 Przypomnienie o spotkaniu</h1>
    <p>Cześć {{userName}}!</p>
    <p>Przypominamy o spotkaniu <strong>{{meetingTitle}}</strong>.</p>
    <p><strong>Rozpoczyna się za:</strong> {{minutesBefore}} minut</p>
    <p><strong>Godzina:</strong> {{meetingTime}}</p>
    <p><strong>Organizator:</strong> {{organizerName}}</p>
    {{#location}}<p><strong>Miejsce:</strong> {{location}}</p>{{/location}}
    {{#meetingLink}}<p><strong>Link:</strong> <a href="{{meetingLink}}">{{meetingLink}}</a></p>{{/meetingLink}}
    <br>
    <p>Do zobaczenia!<br>Zespół MeetHub</p>
</body>
</html>',
 NOW(), NOW())
ON CONFLICT DO NOTHING;

UPDATE email_templates
SET body_template = '<!DOCTYPE html>
 <html>
 <head>
     <meta charset="UTF-8">
     <title>Spotkanie się rozpoczęło</title>
 </head>
 <body>
     <h1>🎉 Spotkanie się rozpoczęło!</h1>
     <p>Cześć {{userName}}!</p>
     <p>Twoje spotkanie <strong>{{meetingTitle}}</strong> właśnie się rozpoczęło.</p>
     <p><strong>Czas:</strong> {{meetingDate}}</p>
     <p>Dołącz do spotkania w aplikacji MeetHub.</p>
     <br>
     <p>Pozdrawiamy,<br>Zespół MeetHub</p>
 </body>
 </html>',
     subject = '🎉 Spotkanie {{meetingTitle}} się rozpoczęło!',
     updated_at = NOW()
 WHERE template_key = 'meeting_started' AND language = 'pl';

UPDATE email_templates
SET body_template = body_template || '

 <div style="background-color: #f8f9fa; padding: 20px; margin: 25px 0;">
     <h3>🔐 Potwierdź obecność</h3>
     <p>Twój kod potwierdzający: <strong>{{attendanceTokenFormatted}}</strong></p>
     <p><a href="{{confirmationLink}}">Kliknij tutaj aby potwierdzić</a></p>
 </div>
'
 WHERE template_key IN ('meeting_started', 'meeting_started_participant');