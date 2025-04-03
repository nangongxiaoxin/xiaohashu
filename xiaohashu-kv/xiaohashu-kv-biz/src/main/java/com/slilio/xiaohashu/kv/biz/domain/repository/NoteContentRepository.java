package com.slilio.xiaohashu.kv.biz.domain.repository;

import com.slilio.xiaohashu.kv.biz.domain.dataobject.NoteContentDO;
import java.util.UUID;
import org.springframework.data.cassandra.repository.CassandraRepository;

public interface NoteContentRepository extends CassandraRepository<NoteContentDO, UUID> {}
