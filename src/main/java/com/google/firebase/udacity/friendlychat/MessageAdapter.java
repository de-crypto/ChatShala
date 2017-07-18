package com.google.firebase.udacity.friendlychat;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MessageAdapter extends ArrayAdapter<FriendlyMessage> {

    static int check;
    static  int arr[] = {0,1};
    public MessageAdapter(Context context, int resource, List<FriendlyMessage> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_message, parent, false);
        }

        ImageView photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
        TextView recmessageTextView = (TextView) convertView.findViewById(R.id.recievemessageTextView);
        TextView recauthorTextView = (TextView) convertView.findViewById(R.id.recievenameTextView);
        TextView sendmessageTextView = (TextView) convertView.findViewById(R.id.sendmessageTextView);
        TextView sendauthorTextView = (TextView) convertView.findViewById(R.id.sendnameTextView);

        FriendlyMessage message = getItem(position);
        ///////// Calender ////////
        Calendar cc = Calendar.getInstance();
        int year=cc.get(Calendar.YEAR);
        int month=cc.get(Calendar.MONTH);
        int mDay = cc.get(Calendar.DAY_OF_MONTH);
        int mHour = cc.get(Calendar.HOUR_OF_DAY);
        int mMinute = cc.get(Calendar.MINUTE);
        String Date = Integer.toString(mDay) + "/"+ Integer.toString(month) + "/" + Integer.toString(year);
        String Time = Integer.toString(mHour) + ":"+ Integer.toString(mMinute);
        ///////// end calender ////////
        boolean isPhoto = message.getPhotoUrl() != null;
        check = getRandom(arr);
        if(check == 0) {
            if (isPhoto) {
                recauthorTextView.setVisibility(View.VISIBLE);
                recmessageTextView.setVisibility(View.GONE);
                sendmessageTextView.setVisibility(View.GONE);
                sendauthorTextView.setVisibility(View.GONE);
                photoImageView.setVisibility(View.VISIBLE);
                Glide.with(photoImageView.getContext())
                        .load(message.getPhotoUrl())
                        .into(photoImageView);
            } else {
                recauthorTextView.setVisibility(View.VISIBLE);
                recmessageTextView.setVisibility(View.VISIBLE);
                sendmessageTextView.setVisibility(View.GONE);
                sendauthorTextView.setVisibility(View.GONE);
                photoImageView.setVisibility(View.GONE);
                recmessageTextView.setText(message.getText());
            }
            recauthorTextView.setText(message.getName()+ " | " + Time + " | " +Date);
        }
        else if (check == 1){
            //flip the changes
            if (isPhoto) {
                recauthorTextView.setVisibility(View.GONE);
                recmessageTextView.setVisibility(View.GONE);
                sendmessageTextView.setVisibility(View.GONE);
                sendauthorTextView.setVisibility(View.VISIBLE);
                photoImageView.setVisibility(View.VISIBLE);
                Glide.with(photoImageView.getContext())
                        .load(message.getPhotoUrl())
                        .into(photoImageView);
            } else {
                recauthorTextView.setVisibility(View.GONE);
                recmessageTextView.setVisibility(View.GONE);
                sendmessageTextView.setVisibility(View.VISIBLE);
                sendauthorTextView.setVisibility(View.VISIBLE);
                photoImageView.setVisibility(View.GONE);
                sendmessageTextView.setText(message.getText());
            }
            sendauthorTextView.setText(message.getName()+ " | " + Time + " | " +Date);
        }
        return convertView;
    }

    public static int getRandom(int[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }


}
