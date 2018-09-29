package com.freeme.bigmodel;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.freeme.bigmodel.filter.Origin;
import com.freeme.gallery.R;
import com.google.gson.Gson;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TranslateActivity extends com.freeme.bigmodel.BlockBaseActivity {
    //add by droi heqianqian
    public static final String APP_ID  = "20160217000012251";
    public static final String APP_KEY = "vsyirugRiJ9UCWfyZ_Rv";
    public static final String AND     = "&";
    public static Button btnTransToEng;
    public static Button btnTransToJep;
    public static Button btnTransToKor;
    //add by tyd zhuya
    public static String oldSelectTra = "en";
    /**
     * @author heqianqian
     * for subtitle translate
     */
    private String inputAfterText;
    //	private AsyncHttpClient client;
    private List<String> chinesel = new ArrayList<String>();
    private List<String> englishl = new ArrayList<String>();
    private EditText chineseet;
    private EditText englishet;
    private ListView mylistview;
    private TextView remainsize;
    private int maxchinese = 30;
    //end
    private int       size;
    private ImageView iv;
    private boolean isCleanAll = true;

    //end
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case 1:
                Intent intent = new Intent(TranslateActivity.this, com.freeme.bigmodel.BlockfilterActivity.class);
                if ("".equals(chineseet.getText().toString()) && "".equals(englishet.getText().toString())) {
                    intent.putExtra("chinese", getString(R.string.chinesecontent));
                    intent.putExtra("english", getString(R.string.englishcontent));
                } else {
                    intent.putExtra("chinese", chineseet.getText().toString());
                    intent.putExtra("english", englishet.getText().toString());
                }
                setResult(0x202, intent);
                finish();
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, getString(R.string.finish) + "  ").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(getString(R.string.returns));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        initdata();
        chineseet.addTextChangedListener(new TextWatcher() {


            @Override
            public void beforeTextChanged(CharSequence s, int start, int end,
                                          int count) {
                // TODO Auto-generated method stub
                inputAfterText = chineseet.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int end, int count) {
                // TODO Auto-generated method stu
                size = maxchinese - (chineseet.getText().length());
                remainsize.setText(getString(R.string.remaincount) + ":" + size);
                if (containsEmoji(chineseet.getText().toString())) {
                    Toast.makeText(TranslateActivity.this, "不支持输入Emoji表情符号", Toast.LENGTH_SHORT).show();
                    chineseet.setText(inputAfterText);
                }
                //chineseet.setSelection(chineseet.getText().toString().length());
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
                //add by tyd zhuya 20151125
                if (isCleanAll) {
                    String str = chineseet.getText().toString();
                    new TransAsyncTask().execute(getUrl(str, "auto", oldSelectTra));
                }
                //end
            }
        });

        chineseet.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View arg0, boolean arg1) {
                // TODO Auto-generated method stub
                String str = chineseet.getText().toString();
                if (arg1 && TextUtils.isEmpty(chineseet.getText()) && TextUtils.isEmpty(englishet.getText())) {
                    chineseet.setHint("");
                    englishet.setHint("");
                    btnTransToEng.setBackgroundResource(R.drawable.translatelanguageselected);
                    //ba.setTextColor(Color.WHITE);
                } else {
                    chineseet.setHint(getString(R.string.chinesecontent));
                    englishet.setHint(getString(R.string.englishcontent));
                    //add by tyd zhuya,after click finish_button remember status
                    switch (oldSelectTra) {
                        case "en":
                            btnTransToEng.setBackgroundResource(R.drawable.translatelanguageselected);
                            break;
                        case "jp":
                            btnTransToJep.setBackgroundResource(R.drawable.translatelanguageselected);
                            break;
                        case "kor":
                            btnTransToKor.setBackgroundResource(R.drawable.translatelanguageselected);
                            break;
                        default:
                            btnTransToEng.setBackgroundResource(R.drawable.translatelanguageselected);
                            break;
                    }

                    if (isCleanAll) {
                        new TransAsyncTask().execute(getUrl(str, "auto", oldSelectTra));
                    }
                    //end
                }
            }
        });

    }

    /**
     * add data for chinesel and englishl
     */
    public void initdata() {
        //client = new AsyncHttpClient(5000);
        mylistview = (ListView) findViewById(R.id.translate_recommand_listview);
        chineseet = (EditText) findViewById(R.id.translate_chinese_et);
        englishet = (EditText) findViewById(R.id.translate_english_et);
        englishet.setEnabled(false);
        if (getIntent().getStringExtra("from").equals(getString(R.string.chinesecontent))) {
            chineseet.setText("");
        } else {
            chineseet.setText(getIntent().getStringExtra("from"));
        }
        if (getIntent().getStringExtra("to").equals(getString(R.string.englishcontent))) {
            englishet.setText("");
        } else {
            englishet.setText(getIntent().getStringExtra("to"));
        }

        size = maxchinese - (chineseet.getText().length());
        remainsize = (TextView) findViewById(R.id.translate_remainchinese_tv);
        remainsize.setText(getString(R.string.remaincount) + ":" + size);
        chineseet.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxchinese)});
        mylistview.setAdapter(new Myadapter());
        chinesel.add(getString(R.string.chinesecontentone));
        englishl.add(getString(R.string.englishcontentone));
        chinesel.add(getString(R.string.chinesecontenttwo));
        englishl.add(getString(R.string.englishcontenttwo));
        chinesel.add(getString(R.string.chinesecontentthree));
        englishl.add(getString(R.string.englishcontentthree));
        chinesel.add(getString(R.string.chinesecontenfour));
        englishl.add(getString(R.string.englishcontentfour));
        chinesel.add(getString(R.string.chinesecontentfive));
        englishl.add(getString(R.string.englishcontentfive));
        btnTransToEng = (Button) findViewById(R.id.translate_english_button);
        btnTransToJep = (Button) findViewById(R.id.translate_japanese_button);
        btnTransToKor = (Button) findViewById(R.id.translate_korean_button);
    }

    /**
     * detection is hava emoji
     *
     * @param source
     * @return
     */
    public static boolean containsEmoji(String source) {
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char codePoint = source.charAt(i);
            if (!isEmojiCharacter(codePoint)) {
                return true;
            }
        }
        return false;
    }

    public static String getUrl(String content, String from, String to) {
        StringBuffer sb = new StringBuffer(
                "http://api.fanyi.baidu.com/api/trans/vip/translate?");
        try {
            sb.append("q=" + URLEncoder.encode(content, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sb.append(AND);
        sb.append("from=" + from);
        sb.append(AND);
        sb.append("to=" + to);
        sb.append(AND);
        sb.append("appid=" + APP_ID);
        sb.append(AND);
        String salt = (System.currentTimeMillis() / 1000) + "";
        sb.append("salt=" + salt);
        sb.append(AND);

        String sign = md5(APP_ID + content + salt + APP_KEY);
        sb.append("sign=" + sign);
        Log.i("heqianqian", "url========" + sb.toString());
        if ("en".equals(to)) {
            btnTransToEng.setBackgroundResource(R.drawable.translatelanguageselected);
            btnTransToJep.setBackgroundResource(R.drawable.translatelanguage);
            btnTransToKor.setBackgroundResource(R.drawable.translatelanguage);
        } else if ("jp".equals(to)) {
            btnTransToJep.setBackgroundResource(R.drawable.translatelanguageselected);
            btnTransToEng.setBackgroundResource(R.drawable.translatelanguage);
            btnTransToKor.setBackgroundResource(R.drawable.translatelanguage);
        } else if ("kor".equals(to)) {
            btnTransToKor.setBackgroundResource(R.drawable.translatelanguageselected);
            btnTransToEng.setBackgroundResource(R.drawable.translatelanguage);
            btnTransToJep.setBackgroundResource(R.drawable.translatelanguage);
        }
        return sb.toString();

    }

    /**
     * is or not emoji
     *
     * @param
     * @return
     */
    private static boolean isEmojiCharacter(char codePoint) {
        return (codePoint == 0x0) || (codePoint == 0x9) || (codePoint == 0xA) ||
                (codePoint == 0xD) || ((codePoint >= 0x20) && (codePoint <= 0xD7FF)) ||
                ((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) || ((codePoint >= 0x10000)
                && (codePoint <= 0x10FFFF));
    }

    //add by droi heqianqian on 20160217
    private static String md5(String string) {
        // TODO Auto-generated method stub
        MessageDigest messageDigest = null;

        try {
            messageDigest = MessageDigest.getInstance("MD5");

            messageDigest.reset();
            messageDigest.update(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException caught!");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        byte[] byteArray = messageDigest.digest();

        StringBuffer md5StrBuff = new StringBuffer();

        for (int i = 0; i < byteArray.length; i++) {
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
                md5StrBuff.append("0").append(
                        Integer.toHexString(0xFF & byteArray[i]));
            else
                md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
        }
        return md5StrBuff.toString().toLowerCase(Locale.getDefault());
    }

    /**
     * reset edittext content
     *
     * @param view
     */
    public void reset(View view) {
        switch (view.getId()) {
            case R.id.translate_resetchinese_button:
                isCleanAll = false;
                chineseet.setText("");
                break;
            case R.id.translate_resetenglish_button:
                btnTransToEng.setBackgroundResource(R.drawable.translatelanguage);
                btnTransToJep.setBackgroundResource(R.drawable.translatelanguage);
                btnTransToKor.setBackgroundResource(R.drawable.translatelanguage);
                englishet.setText("");
                break;
        }
        isCleanAll = true;
    }

    /**
     * commit data to service
     *
     * @param view
     */
    public void sendmsg(View view) {
        String str = chineseet.getText().toString();
//        try {
//            str = URLEncoder.encode(str, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }

        btnTransToEng.setBackgroundResource(R.drawable.translatelanguage);
        btnTransToJep.setBackgroundResource(R.drawable.translatelanguage);
        btnTransToKor.setBackgroundResource(R.drawable.translatelanguage);
        switch (view.getId()) {
            case R.id.translate_english_button:
                view.setBackgroundResource(R.drawable.translatelanguageselected);
                //ba.setTextColor(Color.WHITE);
                oldSelectTra = "en";
                new TransAsyncTask().execute(getUrl(str, "auto", "en"));
                //getdate(str,"en");
                break;
            case R.id.translate_japanese_button:
                //bb.setTextColor(Color.WHITE);
                view.setBackgroundResource(R.drawable.translatelanguageselected);
                oldSelectTra = "jp";
                new TransAsyncTask().execute(getUrl(str, "auto", "jp"));
                //getdate(str,"jp");
                break;
            case R.id.translate_korean_button:
                //bc.setTextColor(Color.WHITE);
                view.setBackgroundResource(R.drawable.translatelanguageselected);
                oldSelectTra = "kor";
                new TransAsyncTask().execute(getUrl(str, "auto", "kor"));
                //getdate(str,"kor");
                break;
        }

    }
    //end

    /**
     * create adapter for listview
     *
     * @author heqianqian
     */
    class Myadapter extends BaseAdapter {
        private LayoutInflater li;
        private TextView       chineseitem;
        private TextView       englishitem;

        public Myadapter() {
            li = (LayoutInflater) TranslateActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return chinesel.size();
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(final int arg0, View arg1, ViewGroup arg2) {
            // TODO Auto-generated method stub
            if (arg1 == null) {
                arg1 = li.inflate(R.layout.activity_remmand_item, null);
            }
            chineseitem = (TextView) arg1.findViewById(R.id.remmandchinese_tv);
            chineseitem.setText(chinesel.get(arg0));
            englishitem = (TextView) arg1.findViewById(R.id.remmandenglish_tv);
            englishitem.setText(englishl.get(arg0));
            iv = (ImageView) arg1.findViewById(R.id.remmand_iv);
            if (arg0 == chinesel.size() - 1) {
                iv.setVisibility(View.GONE);
            }
            arg1.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View view) {
                    // TODO Auto-generated method stub
                    if(!(BlockBaseActivity.activitys.getLast() instanceof TranslateActivity)){
                        BlockBaseActivity.activitys.addLast(TranslateActivity.this);
                    }
                    chineseitem = (TextView) view.findViewById(R.id.remmandchinese_tv);
                    chineseitem.setText(chinesel.get(arg0));
                    englishitem = (TextView) view.findViewById(R.id.remmandenglish_tv);
                    englishitem.setText(englishl.get(arg0));
                    Intent intent = new Intent(TranslateActivity.this, com.freeme.bigmodel.BlockfilterActivity.class);
                    intent.putExtra("chinese", chineseitem.getText().toString());
                    intent.putExtra("english", englishitem.getText().toString());
                    oldSelectTra = "en";
                    setResult(0x202, intent);
                    finish();
                }
            });

            return arg1;
        }

    }

    /**
     * get  translated data from baidu
     */
    class TransAsyncTask extends AsyncTask<String, String, String> {
        //private AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            String myString = "";
            try {
                URL uri = new URL(params[0]);
                URLConnection ucon = uri.openConnection();
                InputStream is = ucon.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                ByteArrayBuffer baf = new ByteArrayBuffer(100);
                int current = 0;
                while ((current = bis.read()) != -1) {
                    baf.append((byte) current);
                }

                myString = new String(baf.toByteArray(), "GBK");
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TranslateActivity.this, getString(R.string.translatefailed), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            return myString;
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            String str = chineseet.getText().toString();
            Gson gson = new Gson();
            Origin og = gson.fromJson(result, Origin.class);
            if (!"".equals(str) && og != null) {
                if (og.getTrans_result() != null) {
                    if (og.getTrans_result().get(0) != null) {
                        englishet.setText(og.getTrans_result().get(0).getDst());
                    }
                }
            } else {
                englishet.setText("");
//                Toast.makeText(TranslateActivity.this, getString(R.string.inputcontent), Toast.LENGTH_SHORT).show();
            }
        }
    }

}
