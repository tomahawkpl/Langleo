package com.atteo.langleo;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.atteo.langleo.algorithms.Olli;
import com.atteo.langleo.models.Collection;
import com.atteo.langleo.models.Language;
import com.atteo.langleo.models.List;
import com.atteo.langleo.models.OlliAnswer;
import com.atteo.langleo.models.OlliFactor;
import com.atteo.langleo.models.Question;
import com.atteo.langleo.models.StudyDay;
import com.atteo.langleo.models.StudySession;
import com.atteo.langleo.models.Word;
import com.atteo.silo.Silo;
import com.atteo.silo.StorableCollection;
import com.atteo.silo.Silo.RunningMode;

public class Langleo extends Application {

	static private LearningAlgorithm learningAlgorithm = null;

	public static final String DEFAULT_NEW_WORDS_PER_DAY = "20";
	public static final String DEFAULT_NEW_WORDS_PER_SESSION = "10";
	public static final long SESSION_TIMEOUT = 1000 * 60 * 15;

	private final String[] MIGRATIONS = new String[] {
			"create table language(id integer primary key autoincrement, name text, shortName text, studystackid integer)",
			"create table collection(id integer primary key autoincrement, name text, targetLanguage_id integer, baseLanguage_id integer, priority integer, started integer, disabled integer)",
			"create table list(id integer primary key autoincrement, name text, collection_id integer, fromstudystack integer)",
			"create table word(id integer primary key autoincrement, word text, translation text, note text, list_id integer, studied integer)",
			"create table question(id integer primary key autoincrement, word_id integer, date integer, difficulty real, repetitions integer, queries integer, correct integer, previousdate integer, previousinterval integer, collection_id integer)",
			"create table studyday(id integer primary key autoincrement, date integer, newwords integer, maxnewwords integer)",
			"create table ollifactor(id integer primary key autoincrement, repetitions integer, difficulty integer, factor real, hits integer)",
			"create table ollianswer(id integer primary key autoincrement, repetitions integer, difficulty integer, factor real, correct integer, incorrect integer)",
			"create table studysession(id integer primary key autoincrement, date integer, newwords integer, maxnewwords integer)",
			"insert into language(name,shortname, studystackid) values ('English','en-us', -1)",
			"insert into language(name,shortname, studystackid) values ('Spanish','es', 14)",
			"insert into language(name,shortname, studystackid) values ('French','fr', 13)",
			"insert into language(name,shortname, studystackid) values ('Afrikaans','af', -1)",
			"insert into language(name,shortname, studystackid) values ('Bosnian','bs', -1)",
			"insert into language(name,shortname, studystackid) values ('Cantonese','zh-yue', 50)",
			"insert into language(name,shortname, studystackid) values ('Mandarin','zh', 50)",
			"insert into language(name,shortname, studystackid) values ('Croatian','hr', -1)",
			"insert into language(name,shortname, studystackid) values ('Czech','cz', 71)",
			"insert into language(name,shortname, studystackid) values ('Dutch','nl', -1)",
			"insert into language(name,shortname, studystackid) values ('Esperanto','eo', 56)",
			"insert into language(name,shortname, studystackid) values ('Finnish','fi', 58)",
			"insert into language(name,shortname, studystackid) values ('German','de', 27)",
			"insert into language(name,shortname, studystackid) values ('Greek','el', 39)",
			"insert into language(name,shortname, studystackid) values ('Hindi','hi', -1)",
			"insert into language(name,shortname, studystackid) values ('Hungarian','hu', -1)",
			"insert into language(name,shortname, studystackid) values ('Icelandic','is', -1)",
			"insert into language(name,shortname, studystackid) values ('Indonesian','id', -1)",
			"insert into language(name,shortname, studystackid) values ('Italian','it', 32)",
			"insert into language(name,shortname, studystackid) values ('Kurdish','ku', -1)",
			"insert into language(name,shortname, studystackid) values ('Latin','la', 24)",
			"insert into language(name,shortname, studystackid) values ('Macedonian','mk', -1)",
			"insert into language(name,shortname, studystackid) values ('Norwegian','no', -1)",
			"insert into language(name,shortname, studystackid) values ('Polish','pl', -1)",
			"insert into language(name,shortname, studystackid) values ('Portugese','pt', 49)",
			"insert into language(name,shortname, studystackid) values ('Romanian','ro', -1)",
			"insert into language(name,shortname, studystackid) values ('Serbian','sr', -1)",
			"insert into language(name,shortname, studystackid) values ('Slovak','sk', 57)",
			"insert into language(name,shortname, studystackid) values ('Swahili','sw', -1)",
			"insert into language(name,shortname, studystackid) values ('Swedish','sv', -1)",
			"insert into language(name,shortname, studystackid) values ('Tamil','ta', -1)",
			"insert into language(name,shortname, studystackid) values ('Turkish','tr', -1)",
			"insert into language(name,shortname, studystackid) values ('Vietnamese','vi', -1)",
			"insert into language(name,shortname, studystackid) values ('Welsh','cy', -1)",
			};

	public static String DATABASE_NAME = "Langleo";
	public static String LOG_IDENT = "Langleo";
	public static String DIR_NAME = "Langleo";
	public static String PACKAGE = "com.atteo.langleo";
	
	private static ArrayList<Language> cachedLanguages;

	private static SharedPreferences sharedPreferences;

	public void onCreate() {
		super.onCreate();

		sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		learningAlgorithm = new Olli();

		openDatabase();

		getLanguages();

		createDirectory();

	}

	public static SharedPreferences getPreferences() {
		return sharedPreferences;
	}

	private void createDirectory() {
		File f = Environment.getExternalStorageDirectory();
		f = new File(f, Langleo.DIR_NAME);
		if (!f.exists()) {
			f.mkdir();
		}
	}

	public static ArrayList<Language> getLanguages() {
		if (cachedLanguages == null)
			cachedLanguages = new StorableCollection(Language.class)
					.orderByInPlace("name").toArrayList();
		return cachedLanguages;
	}

	public static LearningAlgorithm getLearningAlgorithm() {
		return learningAlgorithm;
	}

	public void openDatabase() {
		Silo.open(this, DATABASE_NAME, MIGRATIONS, RunningMode.DEVELOPMENT);
		Silo.initializeClass(Collection.class);
		Silo.initializeClass(List.class);
		Silo.initializeClass(Word.class);
		Silo.initializeClass(Language.class);
		Silo.initializeClass(Question.class);
		Silo.initializeClass(StudyDay.class);
		Silo.initializeClass(StudySession.class);
		Silo.initializeClass(OlliFactor.class);
		Silo.initializeClass(OlliAnswer.class);
		Silo.setLogIdent(LOG_IDENT);
	}
	
	public void closeDatabase() {
		Silo.close();
	}

	public void onTerminate() {
		closeDatabase();
		super.onTerminate();
	}
	
	public static boolean isConnectionAvailable(Context context) {
		NetworkInfo ni = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		boolean connected;
		if (ni == null)
			connected = false;
		else
			connected = ni.isConnected();
		
		if (!connected) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context) {
			};
			builder.setMessage(
					context.getString(R.string.no_internet_connection)).setCancelable(false)
					.setPositiveButton(context.getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {

								}
							});
			AlertDialog alert = builder.create();
			alert.show();
		}
		
		return connected;
	}
	
}
