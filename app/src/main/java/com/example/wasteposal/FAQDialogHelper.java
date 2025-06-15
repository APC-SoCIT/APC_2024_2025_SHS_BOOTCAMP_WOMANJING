package com.example.wasteposal;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.Html;

public class FAQDialogHelper {

    public static void showFAQ(Context context, String message, int imageResId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.faq_popup, null);

        ImageView imageView = dialogView.findViewById(R.id.faqImage);
        TextView textView = dialogView.findViewById(R.id.faqText);

        imageView.setImageResource(imageResId);
        textView.setText(Html.fromHtml(message));

        builder.setView(dialogView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

