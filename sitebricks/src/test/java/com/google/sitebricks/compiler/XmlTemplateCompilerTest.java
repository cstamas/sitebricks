package com.google.sitebricks.compiler;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.sitebricks.*;
import com.google.sitebricks.http.Delete;
import com.google.sitebricks.http.Get;
import com.google.sitebricks.http.Post;
import com.google.sitebricks.http.Put;
import com.google.sitebricks.rendering.EmbedAs;
import com.google.sitebricks.rendering.control.Chains;
import com.google.sitebricks.rendering.control.WidgetRegistry;
import com.google.sitebricks.routing.PageBook;
import com.google.sitebricks.routing.SystemMetrics;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.util.Map;

import static com.google.sitebricks.compiler.HtmlTemplateCompilerTest.mockRequestProviderForContext;
import static org.easymock.EasyMock.createNiceMock;

/**
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 */
public class XmlTemplateCompilerTest {
  private static final String ANNOTATION_EXPRESSIONS = "Annotation expressions";
  private Injector injector;
  private PageBook pageBook;
  private SystemMetrics metrics;
  private final Map<String, Class<? extends Annotation>> methods = Maps.newHashMap();

  @BeforeMethod
  public void pre() {
    methods.put("get", Get.class);
    methods.put("post", Post.class);
    methods.put("put", Put.class);
    methods.put("delete", Delete.class);

    injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bind(HttpServletRequest.class).toProvider(mockRequestProviderForContext());
        bind(new TypeLiteral<Map<String, Class<? extends Annotation>>>() {
        })
            .annotatedWith(Bricks.class)
            .toInstance(methods);
      }
    });

    pageBook = createNiceMock(PageBook.class);
    metrics = createNiceMock(SystemMetrics.class);
  }

  @Test
  public final void annotationKeyExtraction() {
    assert "link".equals(Dom.extractKeyAndContent("@Link")[0]) : "Extraction wrong: ";
    assert "thing".equals(Dom.extractKeyAndContent("@Thing()")[0]) : "Extraction wrong: ";
    assert "thing".equals(Dom.extractKeyAndContent("@Thing(asodkoas)")[0]) : "Extraction wrong: ";
    assert "thing".equals(Dom.extractKeyAndContent("@Thing(asodkoas)  ")[0]) : "Extraction wrong: ";
    assert "thing".equals(Dom.extractKeyAndContent("@Thing(asodkoas)  kko")[0]) : "Extraction wrong: ";

    assert "".equals(Dom.extractKeyAndContent("@Link")[1]) : "Extraction wrong: ";
    final String val = Dom.extractKeyAndContent("@Thing()")[1];
    assert null == (val) : "Extraction wrong: " + val;
    assert "asodkoas".equals(Dom.extractKeyAndContent("@Thing(asodkoas)")[1]) : "Extraction wrong: ";
    assert "asodkoas".equals(Dom.extractKeyAndContent("@Thing(asodkoas)  ")[1]) : "Extraction wrong: ";
    assert "asodkoas".equals(Dom.extractKeyAndContent("@Thing(asodkoas)  kko")[1]) : "Extraction wrong: ";
  }

  @Test
  public final void readShowIfWidgetTrue() {
    final WidgetRegistry registry = injector.getInstance(WidgetRegistry.class);

    final MvelEvaluatorCompiler compiler = new MvelEvaluatorCompiler(TestBackingType.class);
    Renderable widget =
        new XmlTemplateCompiler(Object.class, compiler, registry, pageBook, metrics)
            .compile("<xml>@ShowIf(true)<p>hello</p></xml>");

    assert null != widget : " null ";

    final StringBuilder builder = new StringBuilder();
    final Respond mockRespond = RespondersForTesting.newRespond();
//        final Respond mockRespond = new StringBuilderRespond() {
//            @Override
//            public void write(String text) {
//                builder.append(text);
//            }
//
//            @Override
//            public void write(char text) {
//                builder.append(text);
//            }
//
//            @Override
//            public void chew() {
//                builder.deleteCharAt(builder.length() - 1);
//            }
//        };

    widget.render(new Object(), mockRespond);

    final String value = mockRespond.toString();
//        System.out.println(value);
    assert "<xml><p>hello</p></xml>".equals(value) : "Did not write expected output, instead: " + value;
  }


  @DataProvider(name = ANNOTATION_EXPRESSIONS)
  public Object[][] get() {
    return new Object[][]{
        {"true"},
        {"java.lang.Boolean.TRUE"},
        {"java.lang.Boolean.valueOf('true')"},
//        {"true ? true : true"},   @TODO (BD): Disabled until I actually investigate if this is a valid test.
        {"'x' == 'x'"},
        {"\"x\" == \"x\""},
        {"'hello' instanceof java.io.Serializable"},
        {"true; return true"},
        {" 5 >= 2 "},
    };
  }

  @Test(dataProvider = ANNOTATION_EXPRESSIONS)
  public final void readAWidgetWithVariousExpressions(String expression) {
    final Evaluator evaluator = new MvelEvaluator();

    final WidgetRegistry registry = injector.getInstance(WidgetRegistry.class);


    Renderable widget =
        new XmlTemplateCompiler(Object.class, new MvelEvaluatorCompiler(Object.class), registry, pageBook, metrics)
            .compile(String.format("<xml>@ShowIf(%s)<p>hello</p></xml>", expression));

    assert null != widget : " null ";

    final StringBuilder builder = new StringBuilder();

    final Respond mockRespond = RespondersForTesting.newRespond();

    widget.render(new Object(), mockRespond);

    final String value = mockRespond.toString();
//        System.out.println(value);
    assert "<xml><p>hello</p></xml>".equals(value) : "Did not write expected output, instead: " + value;
  }


  @Test
  public final void readShowIfWidgetFalse() {
    final Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bind(HttpServletRequest.class).toProvider(mockRequestProviderForContext());
      }
    });

    final Evaluator evaluator = new MvelEvaluator();

    final WidgetRegistry registry = injector.getInstance(WidgetRegistry.class);


    Renderable widget =
        new XmlTemplateCompiler(Object.class, new MvelEvaluatorCompiler(Object.class), registry, pageBook, metrics)
            .compile("<xml>@ShowIf(false)<p>hello</p></xml>");

    assert null != widget : " null ";

    final StringBuilder builder = new StringBuilder();

    final Respond mockRespond = RespondersForTesting.newRespond();
    widget.render(new Object(), mockRespond);

    final String value = mockRespond.toString();
    assert "<xml></xml>".equals(value) : "Did not write expected output, instead: " + value;
  }


  @Test
  public final void readTextWidgetValues() {
    final Evaluator evaluator = new MvelEvaluator();
    final Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bind(HttpServletRequest.class).toProvider(mockRequestProviderForContext());
      }
    });


    final WidgetRegistry registry = injector.getInstance(WidgetRegistry.class);


    Renderable widget =
        new XmlTemplateCompiler(Object.class, new MvelEvaluatorCompiler(TestBackingType.class), registry, pageBook, metrics)
            .compile("<xml><div class='${clazz}'>hello <a href='/people/${id}'>${name}</a></div></xml>");

    assert null != widget : " null ";


    final Respond mockRespond = RespondersForTesting.newRespond();

    widget.render(new TestBackingType("Dhanji", "content", 12), mockRespond);

    final String value = mockRespond.toString();
    assert "<xml><div class='content'>hello <a href='/people/12'>Dhanji</a></div></xml>"
        .replaceAll("'", "\"")
        .equals(value) : "Did not write expected output, instead: " + value;
  }

  public static class TestBackingType {
    private String name;
    private String clazz;
    private Integer id;

    public TestBackingType(String name, String clazz, Integer id) {
      this.name = name;
      this.clazz = clazz;
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public String getClazz() {
      return clazz;
    }

    public Integer getId() {
      return id;
    }
  }


  @Test
  public final void readAndRenderRequireWidget() {
    final Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bind(HttpServletRequest.class).toProvider(mockRequestProviderForContext());
        bind(new TypeLiteral<Map<String, Class<? extends Annotation>>>() {
        })
            .annotatedWith(Bricks.class)
            .toInstance(methods);
      }
    });


    final PageBook pageBook = injector.getInstance(PageBook.class);


    final WidgetRegistry registry = injector.getInstance(WidgetRegistry.class);


    Renderable widget =
        new XmlTemplateCompiler(Object.class, new MvelEvaluatorCompiler(TestBackingType.class), registry, pageBook, metrics)
            .compile("<html> <head>" +
                "   @Require <script type='text/javascript' src='my.js'> </script>" +
                "   @Require <script type='text/javascript' src='my.js'> </script>" +
                "</head>" +
                "<div class='${clazz}'>hello <a href='/people/${id}'>${name}</a></div></html>");

    assert null != widget : " null ";

    final Respond respond = RespondersForTesting.newRespond();

    widget.render(new TestBackingType("Dhanji", "content", 12), respond);

    final String value = respond.toString();
    String expected = "<html> <head>" +
        "      <script type='text/javascript' src='my.js'> </script>" +
        "</head>" +
        "<div class='content'>hello <a href='/people/12'>Dhanji</a></div></html>";
    expected = expected.replaceAll("'", "\"");

    assert expected


        .equals(value) : "Did not write expected output, instead: " + value;
  }


  @Test
  public final void readXmlWidget() {

    final WidgetRegistry registry = injector.getInstance(WidgetRegistry.class);

    Renderable widget =
        new XmlTemplateCompiler(Object.class, new MvelEvaluatorCompiler(TestBackingType.class), registry, pageBook, metrics)
            .compile("<xml><div class='${clazz}'>hello</div></xml>");

    assert null != widget : " null ";


    final Respond mockRespond = RespondersForTesting.newRespond();

    widget.render(new TestBackingType("Dhanji", "content", 12), mockRespond);

    final String s = mockRespond.toString();
    assert "<xml><div class=\"content\">hello</div></xml>"
        .equals(s) : "Did not write expected output, instead: " + s;
  }


  @Test
  public final void readXmlWidgetWithChildren() {

    final WidgetRegistry registry = injector.getInstance(WidgetRegistry.class);

    Renderable widget =
        new XmlTemplateCompiler(Object.class, new MvelEvaluatorCompiler(TestBackingType.class), registry, pageBook, metrics)
            .compile("<xml><div class='${clazz}'>hello @ShowIf(false)<a href='/hi/${id}'>hideme</a></div></xml>");

    assert null != widget : " null ";


    final Respond mockRespond = RespondersForTesting.newRespond();

    widget.render(new TestBackingType("Dhanji", "content", 12), mockRespond);

    final String s = mockRespond.toString();
    assert "<xml><div class=\"content\">hello </div></xml>"
        .equals(s) : "Did not write expected output, instead: " + s;
  }

  @EmbedAs(MyEmbeddedPage.MY_FAVE_ANNOTATION)
  public static class MyEmbeddedPage {
    protected static final String MY_FAVE_ANNOTATION = "MyFave";
    private boolean should = true;

    public boolean isShould() {
      return should;
    }

    public void setShould(boolean should) {
      this.should = should;
    }
  }

  @Test
  public final void readEmbedWidgetAndStoreAsPage() {
    final Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bind(HttpServletRequest.class).toProvider(mockRequestProviderForContext());
        bind(new TypeLiteral<Map<String, Class<? extends Annotation>>>() {
        })
            .annotatedWith(Bricks.class)
            .toInstance(methods);
      }
    });
    final PageBook book = injector      //hacky, where are you super-packages!
        .getInstance(PageBook.class);

    book.at("/somewhere", MyEmbeddedPage.class).apply(Chains.terminal());


    final WidgetRegistry registry = injector.getInstance(WidgetRegistry.class);
    registry.addEmbed("myfave");

    Renderable widget =
        new XmlTemplateCompiler(Object.class, new MvelEvaluatorCompiler(TestBackingType.class), registry, book, metrics)
            .compile("<xml><div class='content'>hello @MyFave(should=false)<a href='/hi/${id}'>hideme</a></div></xml>");

    assert null != widget : " null ";

    //tell pagebook to track this as an embedded widget
    book.embedAs(MyEmbeddedPage.class, MyEmbeddedPage.MY_FAVE_ANNOTATION)
        .apply(Chains.terminal());

    final Respond mockRespond = RespondersForTesting.newRespond();

    widget.render(new TestBackingType("Dhanji", "content", 12), mockRespond);

    final String s = mockRespond.toString();
    assert "<xml><div class=\"content\">hello </div></xml>"
        .equals(s) : "Did not write expected output, instead: " + s;
  }


  @Test
  public final void readEmbedWidgetOnly() {
    final Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bind(HttpServletRequest.class).toProvider(mockRequestProviderForContext());
        bind(new TypeLiteral<Map<String, Class<? extends Annotation>>>() {
        })
            .annotatedWith(Bricks.class)
            .toInstance(methods);
      }
    });
    final PageBook book = injector      //hacky, where are you super-packages!
        .getInstance(PageBook.class);


    final WidgetRegistry registry = injector.getInstance(WidgetRegistry.class);
    registry.addEmbed("myfave");

    Renderable widget =
        new XmlTemplateCompiler(Object.class, new MvelEvaluatorCompiler(TestBackingType.class), registry, pageBook, metrics)
            .compile("<xml><div class='content'>hello @MyFave(should=false)<a href='/hi/${id}'>hideme</a></div></xml>");

    assert null != widget : " null ";

    //tell pagebook to track this as an embedded widget
    book.embedAs(MyEmbeddedPage.class, MyEmbeddedPage.MY_FAVE_ANNOTATION)
        .apply(Chains.terminal());

    final Respond mockRespond = RespondersForTesting.newRespond();

    widget.render(new TestBackingType("Dhanji", "content", 12), mockRespond);

    final String s = mockRespond.toString();
    assert "<xml><div class=\"content\">hello </div></xml>"
        .equals(s) : "Did not write expected output, instead: " + s;
  }


  //TODO Fix this test!
//    @Test
//    public final void readEmbedWidgetWithArgs() throws ExpressionCompileException {
//
//        final Evaluator evaluator = new MvelEvaluator();
//        final Injector injector = Guice.createInjector(new AbstractModule() {
//            protected void configure() {
//                bind(HttpServletRequest.class).toProvider(mockRequestProviderForContext());
//            }
//        });
//        final PageBook book = injector.getInstance(PageBook.class);           //hacky, where are you super-packages!
//
//        final WidgetRegistry registry = injector.getInstance(WidgetRegistry.class);
//
//        final MvelEvaluatorCompiler compiler = new MvelEvaluatorCompiler(TestBackingType.class);
//        Renderable widget =
//                new XmlTemplateCompiler(Object.class, compiler, registry, book, metrics)
//                    .compile("<xml><div class='content'>hello @MyFave(should=true)<a href='/hi/${id}'> @With(\"me\")<p>showme</p></a></div></xml>");
//
//        assert null != widget : " null ";
//
//
//        XmlWidget bodyWrapper = new XmlWidget(Chains.proceeding().addWidget(new IncludeWidget(new TerminalWidgetChain(), "'me'", evaluator)),
//                "body", compiler, Collections.<String, String>emptyMap());
//
//        bodyWrapper.setRequestProvider(mockRequestProviderForContext());
//
//        //should include the @With("me") annotated widget from the template above (discarding the <p> tag).
//        book.embedAs(MyEmbeddedPage.class).apply(bodyWrapper);
//
//        final Respond mockRespond = new StringBuilderRespond();
//
//        widget.render(new TestBackingType("Dhanji", "content", 12), mockRespond);
//
//        final String s = mockRespond.toString();
//        assert "<xml><div class=\"content\">hello showme</div></xml>"
//                .equals(s) : "Did not write expected output, instead: " + s;
//    }

}
