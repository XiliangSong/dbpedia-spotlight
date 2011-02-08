package org.dbpedia.spotlight.lucene.similarity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.apache.jcs.engine.control.CompositeCacheManager;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.OpenBitSet;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.util.Properties;

/**
 *
 * The strategy here is to keep as many cached bitsets as possible,
 *
 //TODO This looks like it could break if we have multiple readers, since store the docIdSet for this reader, and reuse it later on with (possibly) other readers. The correct way seems to be to key on both reader and term.
 
 * @author pablomendes
 */
public class JCSTermCache extends TermCache {

    Log LOG = LogFactory.getLog(JCSTermCache.class);

    //Singleton
    private static JCSTermCache instance;   //TODO this could be a map from IndexReader to JCSTermCache
    private static int checkedOut = 0;

    private static JCS termCache;

    public JCSTermCache(LuceneManager mgr) throws CacheException {
        super(mgr);
        init();
    }

    public void init() throws CacheException {           //TODO get these props from dbpa.properties
        CompositeCacheManager ccm = CompositeCacheManager.getUnconfiguredInstance();
        Properties props = new Properties();
        //props.load(/* load properties from some location defined by your app */);
        props.put("jcs.default","");
        props.put("jcs.default.cacheattributes","org.apache.jcs.engine.CompositeCacheAttributes");
        props.put("jcs.default.cacheattributes.MemoryCacheName","org.apache.jcs.engine.memory.lru.LRUMemoryCache");
        props.put("jcs.default.cacheattributes.MaxObjects","10000");
        ccm.configure(props);

        termCache = JCS.getInstance("termCache");
    }

    //public static JCSTermCache getInstance(IndexReader mgr)

    /**
	 * Singleton access point to the manager.
	 */
	public static JCSTermCache getInstance(LuceneManager mgr) throws CacheException {
		synchronized (JCSTermCache.class) {
			if (instance == null) {
				instance = new JCSTermCache(mgr);
			}
		}

		synchronized (instance) {
			instance.checkedOut++;
		}

		return instance;
	}

    /**
     * If you are about to put something in the cache, there is no need to check containsKey before
     * Method put is safe.
     *
     * @param o
     * @return
     */
    @Override
    public boolean containsKey(Term o) {
        boolean contains = false;
        try {
            contains = (termCache.get(o) != null) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contains;
    }

    /**
     * Checks if a term is in the cache and adds the term if not.
     * It is safe to call without testing for containsKey.
     * @param term
     * @param openBitSet
     * @return
     */
    @Override
    public OpenBitSet put(Term term, OpenBitSet openBitSet) {
		OpenBitSet bits = get(term);
		try {
			if(bits == null){
				termCache.put(term, openBitSet);
			}
			else{
				bits= get(term);
			}
		} catch (CacheException e) {
			e.printStackTrace();
		}
        return bits;
    }

    @Override
    public OpenBitSet get(Term o) {
        return (OpenBitSet) termCache.get(o);
    }

}