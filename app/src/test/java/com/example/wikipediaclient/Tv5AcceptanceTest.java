package com.example.wikipediaclient;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import androidx.navigation.fragment.NavHostFragment;
import com.example.wikipediaclient.data.model.*;
import com.example.wikipediaclient.data.remote.*;
import com.example.wikipediaclient.data.repository.ArticleRepository;
import com.example.wikipediaclient.data.local.ReaderSettings;
import com.example.wikipediaclient.ui.article.FixtureArticles;
import com.example.wikipediaclient.ui.navigation.ArticleNavigator;
import com.example.wikipediaclient.ui.source.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashMap;
import java.util.Map;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, application=Tv5AcceptanceTest.TestApplication.class)
public class Tv5AcceptanceTest {
    public static class TestApplication extends WikiApplication {
        final Map<String,Integer> calls = new HashMap<>();
        @Override protected com.example.wikipediaclient.data.AppContainer createContainer() {
            ArticleRepository articles = new ArticleRepository() {
                @Override public Cancelable getContent(String title, ApiCallback<ArticleContent> callback) {
                    int count=calls.getOrDefault(title,0)+1; calls.put(title,count);
                    if (title.startsWith("Error")) {
                        int code=title.equals("Error404")?404:title.equals("Error429")?429:title.equals("Error503")?503:-1;
                        WikiApiException.Kind kind=title.equals("ErrorJSON")?WikiApiException.Kind.DATA
                                :title.equals("ErrorTimeout")?WikiApiException.Kind.TIMEOUT:WikiApiException.Kind.NO_CONNECTION;
                        if(title.equals("ErrorRetry") && count>1) callback.onSuccess(FixtureArticles.article("Khôi phục"));
                        else callback.onError(new WikiApiException(kind,"Controlled fixture",null,code));
                    } else if(title.equals("Alias")) callback.onSuccess(content("Hà Nội", "https://vi.wikipedia.org/wiki/Ha_Noi_canonical", "CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/"));
                    else callback.onSuccess(FixtureArticles.article(title));
                    return () -> {};
                }
                @Override public Cancelable getSummary(String title, ApiCallback<ArticleSummary> callback) {return () -> {};}
            };
            return new com.example.wikipediaclient.data.AppContainer(this,(q,o,c)->()->{},articles);
        }
    }
    private ActivityController<MainActivity> lifecycle;
    private MainActivity activity;
    private Button share,original;
    private TextView license;
    static ArticleContent content(String title,String url,String label,String link) {
        return new ArticleContent(new PersonalArticle("vi",11,title,url),"<p>Tiếng Việt</p>",123,label,link);
    }
    @Before public void setup() {
        lifecycle=Robolectric.buildActivity(MainActivity.class).setup(); activity=lifecycle.get(); idle();
    }
    @After public void close(){if(lifecycle!=null)lifecycle.pause().stop().destroy();}
    private void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
    private void settings(){((BottomNavigationView)activity.findViewById(R.id.bottom_navigation)).setSelectedItemId(R.id.settingsFragment);idle();}
    private ArticleSourceController component(){
        share=new Button(activity);original=new Button(activity);license=new TextView(activity);
        return new ArticleSourceController(activity,share,original,license);
    }
    private void navigate(String title){
        NavHostFragment host=(NavHostFragment)activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        ArticleNavigator.open(host.getChildFragmentManager().getPrimaryNavigationFragment(),new ArticleLocator("vi",title,null));idle();
    }
    private void committed(){
        WebView web=activity.findViewById(R.id.article_body);
        org.robolectric.shadows.ShadowWebView shadow=Shadows.shadowOf(web);
        shadow.getWebViewClient().onPageCommitVisible(web,shadow.getLastLoadDataWithBaseURL().baseUrl);idle();
    }
    private String nextUrl(){Intent next=Shadows.shadowOf(activity).getNextStartedActivity();assertNotNull(next);return next.getDataString();}

    @Test public void fourFontLevelsPersistAndExposeUsefulAccessibleLabels(){
        settings();int[] ids={R.id.btn_font_small,R.id.btn_font_normal,R.id.btn_font_large,R.id.btn_font_huge};int[] sizes={14,16,18,22};
        for(int i=0;i<ids.length;i++){
            TextView option=activity.findViewById(ids[i]);option.performClick();
            assertEquals(sizes[i],new ReaderSettings(activity).getFontSize());assertTrue(option.isSelected());
            assertTrue(option.getContentDescription().toString().contains(String.valueOf(sizes[i])));
            assertTrue(option.getMinimumHeight()>=48*activity.getResources().getDisplayMetrics().density);
        }
        lifecycle.recreate();idle();activity=lifecycle.get();
        assertTrue(activity.findViewById(R.id.btn_font_huge).isSelected());
    }
    @Test public void aboutSeparatesContentFromAppCodeAndShowsProvidedTeamDetails(){
        settings();assertEquals(activity.getString(R.string.settings_team_val), ((TextView)activity.findViewById(R.id.tv_team_details)).getText().toString());
        assertTrue(((TextView)activity.findViewById(R.id.tv_app_license)).getText().toString().contains("chưa công bố"));
        int[] ids={R.id.btn_about_source,R.id.btn_about_terms,R.id.btn_about_project};
        String[] urls={"https://vi.wikipedia.org/","https://foundation.wikimedia.org/wiki/Policy:Terms_of_Use","https://github.com/xthien666/Wikipedia-Client"};
        for(int i=0;i<ids.length;i++){activity.findViewById(ids[i]).performClick();assertEquals(urls[i],nextUrl());}
    }
    @Test public void sourceComponentUsesBMetadataForShareOriginalAndLicense(){
        ArticleSourceController c=component();c.bind(FixtureArticles.article("A"),true);
        String canonical="https://vi.wikipedia.org/wiki/Resolved_B";
        c.bind(content("B có dấu",canonical,"Giấy phép B","https://creativecommons.org/licenses/by-sa/4.0/"),true);
        share.performClick();Intent chooser=Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(Intent.ACTION_CHOOSER,chooser.getAction());Intent send=chooser.getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals("text/plain",send.getType());assertEquals(canonical,send.getStringExtra(Intent.EXTRA_TEXT));
        assertEquals("B có dấu",send.getStringExtra(Intent.EXTRA_SUBJECT));
        original.performClick();assertEquals(canonical,nextUrl());license.performClick();
        assertEquals("https://creativecommons.org/licenses/by-sa/4.0/",nextUrl());
        assertTrue(license.getContentDescription().toString().contains("B có dấu"));
    }
    @Test public void pendingOrErrorStateCannotShareThePreviousArticle(){
        ArticleSourceController c=component();c.bind(FixtureArticles.article("A"),true);
        c.bind(FixtureArticles.article("B"),false);
        assertFalse(share.isEnabled());assertFalse(original.isEnabled());assertEquals(View.GONE,license.getVisibility());
        c.bind(null,false);share.performClick();original.performClick();license.performClick();
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());assertEquals("",license.getText().toString());
    }
    @Test public void absentOrUnsafeLicenseDoesNotInheritAnOlderLink(){
        ArticleSourceController c=component();c.bind(FixtureArticles.article("A"),true);
        c.bind(content("B","https://vi.wikipedia.org/wiki/B",null,null),true);
        assertFalse(license.isClickable());assertTrue(license.getText().toString().contains("Chưa có"));
        c.bind(content("C","https://vi.wikipedia.org/wiki/C","License C","javascript:alert(1)"),true);
        assertFalse(license.isClickable());license.performClick();assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
    }
    @Test public void unsafeCanonicalUrlsDisableActionsInsteadOfOpeningAnotherHost(){
        ArticleSourceController c=component();
        for(String url:new String[]{"http://vi.wikipedia.org/wiki/A","javascript:alert(1)","https://vi.wikipedia.org.evil.example/wiki/A","https://user:pass@vi.wikipedia.org/wiki/A"}){
            c.bind(content("A",url,"License",null),true);assertFalse(share.isEnabled());assertFalse(original.isEnabled());
        }
    }
    @Test public void missingExternalHandlerShowsMessageWithoutCrashing(){
        Context context=new ContextWrapper(activity){@Override public void startActivity(Intent value){throw new ActivityNotFoundException();}};
        ExternalActions.openUrl(context,"https://vi.wikipedia.org/wiki/A");
        assertEquals(activity.getString(R.string.no_external_app),org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
    }
    @Test public void releaseClearsMetadataAndClickHandlers(){
        ArticleSourceController c=component();c.bind(FixtureArticles.article("A"),true);c.release();
        share.performClick();original.performClick();license.performClick();assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
    }
    @Test public void aliasAndBackKeepCanonicalSourceAndLicenseAligned(){
        activity.findViewById(R.id.open_sample).performClick();idle();committed();
        String titleA=((TextView)activity.findViewById(R.id.article_title)).getText().toString();
        navigate("Alias");assertFalse(activity.findViewById(R.id.action_share).isEnabled());committed();
        assertEquals("Hà Nội",((TextView)activity.findViewById(R.id.article_title)).getText().toString());
        activity.findViewById(R.id.action_share).performClick();Intent send=Shadows.shadowOf(activity).getNextStartedActivity().getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals("https://vi.wikipedia.org/wiki/Ha_Noi_canonical",send.getStringExtra(Intent.EXTRA_TEXT));
        activity.findViewById(R.id.btn_view_original).performClick();assertEquals("https://vi.wikipedia.org/wiki/Ha_Noi_canonical",nextUrl());
        activity.findViewById(R.id.article_license).performClick();assertEquals("https://creativecommons.org/licenses/by-sa/4.0/",nextUrl());
        activity.getOnBackPressedDispatcher().onBackPressed();idle();committed();
        assertEquals(titleA,((TextView)activity.findViewById(R.id.article_title)).getText().toString());
        activity.findViewById(R.id.btn_view_original).performClick();assertEquals(FixtureArticles.article(titleA).article.getCanonicalUrl(),nextUrl());
    }
    @Test public void resolvedAliasIdAvoidsDuplicateReaderAfterRecreation(){
        navigate("Alias");committed();
        lifecycle.recreate();idle();activity=lifecycle.get();committed();
        NavHostFragment host=(NavHostFragment)activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        ArticleNavigator.open(host.getChildFragmentManager().getPrimaryNavigationFragment(),new ArticleLocator("vi","Hà Nội",11L));idle();
        activity.getOnBackPressedDispatcher().onBackPressed();idle();
        assertNotNull(activity.findViewById(R.id.query_input));
    }
    @Test public void resolvedAliasTitleAvoidsDuplicateWithoutPageId(){
        navigate("Alias");committed();navigate("Hà Nội");
        activity.getOnBackPressedDispatcher().onBackPressed();idle();
        assertNotNull(activity.findViewById(R.id.query_input));
    }
    @Test public void t05TimeoutOfflineAndBadJsonDisableSourceAndOfferRetry(){
        for(String title:new String[]{"ErrorTimeout","ErrorOffline","ErrorJSON"}){
            navigate(title);assertFalse(activity.findViewById(R.id.action_share).isEnabled());
            assertEquals(View.GONE,activity.findViewById(R.id.article_license).getVisibility());
            assertEquals(View.VISIBLE,activity.findViewById(R.id.status_action).getVisibility());
            activity.getOnBackPressedDispatcher().onBackPressed();idle();
        }
    }
    @Test public void t05MissingAndRateLimitedArticlesDoNotRetryAutomatically(){
        for(String title:new String[]{"Error404","Error429","Error503"}){
            navigate(title);assertEquals(View.GONE,activity.findViewById(R.id.status_action).getVisibility());
            assertFalse(activity.findViewById(R.id.action_share).isEnabled());
            assertEquals(Integer.valueOf(1),((TestApplication)activity.getApplication()).calls.get(title));
            activity.getOnBackPressedDispatcher().onBackPressed();idle();
        }
    }
    @Test public void t05RetryCanRecoverAndOnlyEnableShareAfterHtmlCommit(){
        navigate("ErrorRetry");activity.findViewById(R.id.status_action).performClick();idle();
        assertFalse(activity.findViewById(R.id.action_share).isEnabled());committed();
        assertTrue(activity.findViewById(R.id.action_share).isEnabled());
        assertEquals("Khôi phục",((TextView)activity.findViewById(R.id.article_title)).getText().toString());
    }
}
