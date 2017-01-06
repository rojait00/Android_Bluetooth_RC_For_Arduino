package de.rojait.control_forarduino.bluetoothjoystickforarduino;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.TextView;

import org.w3c.dom.Text;


public class Einstellungen extends ActionBarActivity {

    public void onStop()
    {
        SharedPreferences pos;
        String fileName = "Settings.st";

        pos = getSharedPreferences(fileName, 0);
        SharedPreferences.Editor editor = pos.edit();
        CheckBox cb = (CheckBox)findViewById(R.id.cbJumpZero);
        TextView tvl = (TextView)findViewById((R.id.Hz_Max_li));
        TextView tvHm = (TextView)findViewById((R.id.Hz_mi));
        TextView tvr = (TextView)findViewById((R.id.Hz_Max_R));

        TextView tvo = (TextView)findViewById((R.id.Ve_Max_O));
        TextView tvVm = (TextView)findViewById((R.id.Ve_Mi));
        TextView tvu = (TextView)findViewById((R.id.Ve_Max_U));

        TextView btID = (TextView)findViewById(R.id.blueT_m_ID);

        try
        {
        String str = Boolean.toString(cb.isChecked());
        editor.putString("JumpToZero", str);
        }
        catch (Exception e)
        {}

        editor.putString("BT_ID",btID.getText().toString());

        editor.putString("Hz_Max_L",  tvl.getText().toString());
        editor.putString("Hz_M",  tvHm.getText().toString());
        editor.putString("Hz_Max_R",  tvr.getText().toString());

        editor.putString("Ve_Max_O",  tvo.getText().toString());
        editor.putString("Ve_M",  tvVm.getText().toString());
        editor.putString("Ve_Max_U",  tvu.getText().toString());
        editor.commit();
        super.onStop();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_einstellungen);

        SharedPreferences pos;
        String fileName = "Settings.st";
        pos = getSharedPreferences(fileName, 0);

        CheckBox cb = (CheckBox)findViewById(R.id.cbJumpZero);
        TextView tvl = (TextView)findViewById((R.id.Hz_Max_li));
        TextView tvHm = (TextView)findViewById((R.id.Hz_mi));
        TextView tvr = (TextView)findViewById((R.id.Hz_Max_R));

        TextView tvo = (TextView)findViewById((R.id.Ve_Max_O));
        TextView tvVm = (TextView)findViewById((R.id.Ve_Mi));
        TextView tvu = (TextView)findViewById((R.id.Ve_Max_U));

        String str = pos.getString("JumpToZero", "true");
        boolean bool = Boolean.parseBoolean(str);
        cb.setChecked(bool);

       tvl.setText( pos.getString("Hz_Max_L", getString(R.string.Horizontal_Links_Max)));
       tvHm.setText(pos.getString("Hz_M", getString(R.string.Horizontal_Mitte)));
       tvr.setText(pos.getString("Hz_Max_R", getString(R.string.Horizontal_Max_Rechts)));

        tvo.setText( pos.getString("Ve_Max_O", getString(R.string.Vertikal_Max_Oben)));
        tvVm.setText( pos.getString("Ve_M", getString(R.string.Vertikal_Mitte)));
        tvu.setText( pos.getString("Ve_Max_U", getString(R.string.Vertikal_Max_Unten)));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_einstellungen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.save) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
