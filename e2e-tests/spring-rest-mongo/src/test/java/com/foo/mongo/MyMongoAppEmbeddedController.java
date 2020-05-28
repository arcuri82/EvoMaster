package com.foo.mongo;

import com.foo.spring.rest.mongo.SpringRestMongoController;

public class MyMongoAppEmbeddedController extends SpringRestMongoController {

    public MyMongoAppEmbeddedController() {
        super(MongoFooApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.mongo";
    }


}
