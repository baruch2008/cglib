/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package net.sf.cglib.proxy;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import net.sf.cglib.core.*;
import net.sf.cglib.reflect.*;

/**
 * Classes generated by {@link Enhancer} pass this object to the
 * registered {@link MethodInterceptor} objects when an intercepted method is invoked. It can
 * be used to either invoke the original method, or call the same method on a different
 * object of the same type.
 * @version $Id: MethodProxy.java,v 1.11 2004/05/09 19:04:01 herbyderby Exp $
 */
public class MethodProxy {
    private Signature sig;
    private String superName;
    private FastClass f1;
    private FastClass f2;
    private int i1;
    private int i2;

    /**
     * For internal use by {@link Enhancer} only; see the {@link net.sf.cglib.reflect.FastMethod} class
     * for similar functionality.
     */
    public static MethodProxy create(ClassLoader loader, Class c1, Class c2, String desc, String name1, String name2) {
        final Signature sig1 = new Signature(name1, desc);
        Signature sig2 = new Signature(name2, desc);
        FastClass f1 = helper(loader, c1);
        FastClass f2 = helper(loader, c2);
        int i1 = f1.getIndex(sig1);
        int i2 = f2.getIndex(sig2);

        MethodProxy proxy;
        if (i1 < 0) {
            proxy = new MethodProxy() {
                public Object invoke(Object obj, Object[] args) throws Throwable {
                    throw new IllegalArgumentException("Protected method: " + sig1);
                }
            };
        } else {
            proxy = new MethodProxy();
        }

        proxy.f1 = f1;
        proxy.f2 = f2;
        proxy.i1 = i1;
        proxy.i2 = i2;
        proxy.sig = sig1;
        proxy.superName = name2;
        return proxy;
    }

    private static FastClass helper(ClassLoader loader, Class type) {
        FastClass.Generator g = new FastClass.Generator();
        g.setType(type);
        g.setClassLoader(loader);
        AbstractClassGenerator fromEnhancer = AbstractClassGenerator.getCurrent();
        if (fromEnhancer != null) {
            g.setNamingPolicy(fromEnhancer.getNamingPolicy());
            g.setStrategy(fromEnhancer.getStrategy());
            g.setAttemptLoad(fromEnhancer.getAttemptLoad());
        }
        return g.create();
    }

    private MethodProxy() {
    }

    /**
     * Return the signature of the proxied method.
     */
    public Signature getSignature() {
        return sig;
    }

    /**
     * Return the name of the synthetic method created by CGLIB which is
     * used by {@link #invokeSuper} to invoke the superclass
     * (non-intercepted) method implementation. The parameter types are
     * the same as the proxied method.
     */
    public String getSuperName() {
        return superName;
    }

    /**
     * Return the {@link net.sf.cglib.reflect.FastClass} method index
     * for the method used by {@link #invokeSuper}. This index uniquely
     * identifies the method within the generated proxy, and therefore
     * can be useful to reference external metadata.
     * @see #getSuperName
     */
    public int getSuperIndex() {
        return i2;
    }

    /**
     * Return the <code>MethodProxy</code> used when intercepting the method
     * matching the given signature.
     * @param type the class generated by Enhancer
     * @param sig the signature to match
     * @return the MethodProxy instance, or null if no applicable matching method is found
     * @throws IllegalArgumentException if the Class was not created by Enhancer or does not use a MethodInterceptor
     */
    public static MethodProxy find(Class type, Signature sig) {
        try {
            Method m = type.getDeclaredMethod(MethodInterceptorGenerator.FIND_PROXY_NAME,
                                              MethodInterceptorGenerator.FIND_PROXY_TYPES);
            return (MethodProxy)m.invoke(null, new Object[]{ sig });
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + type + " does not use a MethodInterceptor");
        } catch (IllegalAccessException e) {
            throw new CodeGenerationException(e);
        } catch (InvocationTargetException e) {
            throw new CodeGenerationException(e);
        }
    }

    /**
     * Invoke the original method, on a different object of the same type.
     * @param obj the compatible object; recursion will result if you use the object passed as the first
     * argument to the MethodInterceptor (usually not what you want)
     * @param args the arguments passed to the intercepted method; you may substitute a different
     * argument array as long as the types are compatible
     * @see MethodInterceptor#intercept
     * @throws Throwable the bare exceptions thrown by the called method are passed through
     * without wrapping in an <code>InvocationTargetException</code>
     */
    public Object invoke(Object obj, Object[] args) throws Throwable {
        try {
            return f1.invoke(i1, obj, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * Invoke the original (super) method on the specified object.
     * @param obj the enhanced object, must be the object passed as the first
     * argument to the MethodInterceptor
     * @param args the arguments passed to the intercepted method; you may substitute a different
     * argument array as long as the types are compatible
     * @see MethodInterceptor#intercept
     * @throws Throwable the bare exceptions thrown by the called method are passed through
     * without wrapping in an <code>InvocationTargetException</code>
     */
    public Object invokeSuper(Object obj, Object[] args) throws Throwable {
        try {
            return f2.invoke(i2, obj, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
