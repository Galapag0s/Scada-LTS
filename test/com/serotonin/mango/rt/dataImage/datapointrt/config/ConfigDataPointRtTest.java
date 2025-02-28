package com.serotonin.mango.rt.dataImage.datapointrt.config;

import br.org.scadabr.db.utils.TestUtils;
import com.serotonin.mango.Common;
import com.serotonin.mango.db.dao.DataPointDao;
import com.serotonin.mango.db.dao.DataSourceDao;
import com.serotonin.mango.rt.EventManager;
import com.serotonin.mango.rt.RuntimeManager;
import com.serotonin.mango.rt.dataImage.DataPointRT;
import com.serotonin.mango.rt.dataImage.DataPointSyncMode;
import com.serotonin.mango.rt.dataImage.PointValueTime;
import com.serotonin.mango.rt.dataImage.types.MangoValue;
import com.serotonin.mango.rt.dataSource.virtual.VirtualDataSourceRT;
import com.serotonin.mango.rt.maint.BackgroundProcessing;
import com.serotonin.mango.util.timeout.TimeoutTask;
import com.serotonin.mango.vo.DataPointVO;
import com.serotonin.mango.vo.User;
import com.serotonin.mango.vo.dataSource.DataSourceVO;
import com.serotonin.mango.vo.dataSource.virtual.ChangeTypeVO;
import com.serotonin.mango.vo.dataSource.virtual.VirtualDataSourceVO;
import com.serotonin.mango.vo.dataSource.virtual.VirtualPointLocatorVO;
import com.serotonin.mango.web.ContextWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.scada_lts.dao.DAO;
import org.scada_lts.dao.model.point.PointValueAdnnotation;
import org.scada_lts.dao.pointvalues.PointValueAdnnotationsDAO;
import org.scada_lts.dao.pointvalues.PointValueDAO;
import org.scada_lts.mango.service.DataPointService;
import org.scada_lts.mango.service.DataSourceService;
import org.scada_lts.mango.service.SystemSettingsService;
import org.springframework.jdbc.core.JdbcTemplate;
import utils.PointValueDAOMemory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest({DAO.class, Common.class, PointValueDAO.class, PointValueAdnnotationsDAO.class,
        DataPointDao.class, DataSourceDao.class, VirtualDataSourceRT.class, RuntimeManager.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*", "com.sun.org.apache.xalan.*",
        "javax.activation.*", "javax.management.*"})
public class ConfigDataPointRtTest {

    private static final Log LOG = LogFactory.getLog(ConfigDataPointRtTest.class);

    private static final PointValueDAOMemory pointValueDAOMemory = new PointValueDAOMemory();
    private static final int NUMBER_OF_TESTS = 20;

    private final PointValueTime oldValue;
    private final PointValueTime oldValueWithUser;
    private final PointValueTime newValue;
    private final PointValueTime newValueWithUser;
    private final PointValueTime newValue2;
    private final PointValueTime newValueWithUser2;
    private final int dataTypeId;
    private final String dataType;
    private final String startValue;
    private final double tolerance;

    private final int defaultCacheSize = 30;
    private final int numberOfLaunches = 10;
    private final User user;
    private final DataPointSyncMode sync;

    private RuntimeManager runtimeManagerMock;
    private DataSourceVO dataSourceVO;
    private DataPointVO dataPointVO;

    public ConfigDataPointRtTest(DataPointSyncMode sync, Object oldValue, Object newValue, Object newValue2,
                                 int dataTypeId, String dataType, String startValue) {
        this.oldValue = new PointValueTime(MangoValue.objectToValue(oldValue), System.currentTimeMillis());
        this.newValue = new PointValueTime(MangoValue.objectToValue(newValue), System.currentTimeMillis() + 10);
        this.dataTypeId = dataTypeId;
        this.dataType = dataType;
        this.startValue = startValue;
        this.tolerance = 0.0;
        this.user = TestUtils.newUser(123);
        this.oldValueWithUser = new PointValueTime(this.oldValue.getValue(), this.oldValue.getTime());
        this.oldValueWithUser.setWhoChangedValue(user.getUsername());
        this.newValueWithUser = new PointValueTime(this.newValue.getValue(), this.newValue.getTime());
        this.newValueWithUser.setWhoChangedValue(user.getUsername());
        this.newValue2 = new PointValueTime(MangoValue.objectToValue(newValue2), System.currentTimeMillis() + 15);
        this.newValueWithUser2 = new PointValueTime(this.newValue2.getValue(), this.newValue2.getTime());
        this.newValueWithUser2.setWhoChangedValue(user.getUsername());
        this.sync = sync;
        config();
    }

    public ConfigDataPointRtTest(DataPointSyncMode sync, Object oldValue, Object newValue, Object newValue2,
                                 int dataTypeId, String dataType, String startValue, double tolerance) {

        this.oldValue = new PointValueTime(MangoValue.objectToValue(oldValue), System.currentTimeMillis());
        this.newValue = new PointValueTime(MangoValue.objectToValue(newValue), System.currentTimeMillis() + 10);
        this.dataTypeId = dataTypeId;
        this.dataType = dataType;
        this.startValue = startValue;
        this.tolerance = tolerance;
        this.user = TestUtils.newUser(123);
        this.oldValueWithUser = new PointValueTime(this.oldValue.getValue(), this.oldValue.getTime());
        this.oldValueWithUser.setWhoChangedValue(user.getUsername());
        this.newValueWithUser = new PointValueTime(this.newValue.getValue(), this.newValue.getTime());
        this.newValueWithUser.setWhoChangedValue(user.getUsername());
        this.newValue2 = new PointValueTime(MangoValue.objectToValue(newValue2), System.currentTimeMillis() + 15);
        this.newValueWithUser2 = new PointValueTime(this.newValue2.getValue(), this.newValue2.getTime());
        this.newValueWithUser2.setWhoChangedValue(user.getUsername());
        this.sync = sync;
        config();

    }

    private void config() {
        try {
            preconfig();
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    private void preconfig() throws Exception {
        DAO dao = mock(DAO.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(dao.getJdbcTemp()).thenReturn(jdbcTemplate);
        whenNew(DAO.class)
                .withNoArguments()
                .thenReturn(dao);

        dataSourceVO = new VirtualDataSourceVO();
        dataSourceVO.setEnabled(true);
        dataSourceVO.setId(567);
        dataSourceVO.setName("test_ds");
        dataSourceVO.setXid("test_xid");

        VirtualPointLocatorVO virtualPointLocatorVO = new VirtualPointLocatorVO();
        virtualPointLocatorVO.setDataTypeId(dataTypeId);
        virtualPointLocatorVO.setChangeTypeId(ChangeTypeVO.Types.NO_CHANGE);
        virtualPointLocatorVO.getNoChange().setStartValue(startValue);

        dataPointVO = new DataPointVO(DataPointVO.LoggingTypes.ON_CHANGE);
        dataPointVO.setId(321);
        dataPointVO.setDefaultCacheSize(defaultCacheSize);
        dataPointVO.setTolerance(tolerance);
        dataPointVO.setPointLocator(virtualPointLocatorVO);
        dataPointVO.setEventDetectors(new ArrayList<>());
        dataPointVO.setEnabled(true);
        dataPointVO.setDataSourceId(dataSourceVO.getId());
        dataPointVO.setDataSourceName(dataSourceVO.getName());
        dataPointVO.setDeviceName(dataSourceVO.getName());

        PointValueAdnnotationsDAO pointValueAdnnotationsDAO = mock(PointValueAdnnotationsDAO.class);
        when(pointValueAdnnotationsDAO.create(any(PointValueAdnnotation.class))).thenAnswer(a -> {
            Object[] args = a.getArguments();
            return pointValueDAOMemory.create((PointValueAdnnotation)args[0]);
        });
        whenNew(PointValueAdnnotationsDAO.class)
                .withNoArguments()
                .thenReturn(pointValueAdnnotationsDAO);

        PointValueDAO pointValueDAO = mock(PointValueDAO.class);
        when(pointValueDAO.create(anyInt(), anyInt(), anyDouble(), anyLong()))
                .thenAnswer(a -> {
                    Object[] args = a.getArguments();
                    return pointValueDAOMemory.create((int)args[0], (int)args[1], (double)args[2], (long)args[3]);
                });
        when(pointValueDAO.getPointValue(anyLong())).thenAnswer(a -> {
            Object[] args = a.getArguments();
            return pointValueDAOMemory.getPointValue((long)args[0]);
        });
        when(pointValueDAO.applyBounds(anyDouble())).thenCallRealMethod();
        whenNew(PointValueDAO.class)
                .withNoArguments()
                .thenReturn(pointValueDAO);

        ContextWrapper contextWrapper = mock(ContextWrapper.class);
        Common.ctx = contextWrapper;

        RuntimeManager runtimeManager = new RuntimeManager();
        runtimeManagerMock = mock(RuntimeManager.class);
        doAnswer(a -> {
            runtimeManager.saveDataPoint((DataPointVO)a.getArguments()[0]);
            return null;
        }).when(runtimeManagerMock).saveDataPoint(any(DataPointVO.class));

        doAnswer(a -> {
            runtimeManager.saveDataSource((DataSourceVO)a.getArguments()[0]);
            return null;
        }).when(runtimeManagerMock).saveDataSource(any(DataSourceVO.class));

        when(runtimeManagerMock.getDataPoint(anyInt()))
                .thenAnswer(a -> runtimeManager.getDataPoint((int)a.getArguments()[0]));
        when(contextWrapper.getRuntimeManager()).thenReturn(runtimeManagerMock);

        BackgroundProcessing backgroundProcessing = mock(BackgroundProcessing.class);
        when(contextWrapper.getBackgroundProcessing()).thenReturn(backgroundProcessing);

        EventManager eventManager = mock(EventManager.class);
        when(contextWrapper.getEventManager()).thenReturn(eventManager);

        DataPointService dataPointService = mock(DataPointService.class);
        whenNew(DataPointService.class)
                .withNoArguments()
                .thenReturn(dataPointService);
        when(dataPointService.getDataPoint(anyInt())).thenReturn(dataPointVO);

        DataSourceService dataSourceService = mock(DataSourceService.class);
        whenNew(DataSourceService.class)
                .withNoArguments()
                .thenReturn(dataSourceService);
        when(dataSourceService.getDataSource(anyInt())).thenReturn(dataSourceVO);

        TimeoutTask timeoutTask = mock(TimeoutTask.class);
        whenNew(TimeoutTask.class)
                .withAnyArguments()
                .thenReturn(timeoutTask);

        SystemSettingsService systemSettingsService = mock(SystemSettingsService.class);
        when(systemSettingsService.getDataPointRtValueSynchronized()).thenReturn(sync);
        whenNew(SystemSettingsService.class)
                .withNoArguments()
                .thenReturn(systemSettingsService);
    }

    @After
    public void clean() {
        pointValueDAOMemory.clear();
    }

    protected static int getNumberOfTests() {
        return NUMBER_OF_TESTS;
    }

    protected DataPointRT start() {
        runtimeManagerMock.saveDataSource(dataSourceVO);
        runtimeManagerMock.saveDataPoint(dataPointVO);
        return runtimeManagerMock.getDataPoint(dataPointVO.getId());
    }

    protected List<PointValueTime> getPointValuesWithUserExpected(List<Double> exepected) {
        PointValueTime oldValue = getOldValueWithUser();
        PointValueTime newValue = getNewValueWithUser();
        PointValueTime newValue2 = getNewValueWithUser2();

        List<PointValueTime> pointValuesExpected = new ArrayList<>();
        pointValuesExpected.add(oldValue);
        for(double i: exepected) {
            if(oldValue.getValue().equals(MangoValue.objectToValue(i))) {
                continue;
            }
            if(newValue.getValue().equals(MangoValue.objectToValue(i))) {
                pointValuesExpected.add(newValue);
            } else if(newValue2.getValue().equals(MangoValue.objectToValue(i))) {
                pointValuesExpected.add(newValue2);
            }
        }
        pointValuesExpected.sort(Comparator.comparing(PointValueTime::getTime).reversed());
        return pointValuesExpected;
    }

    protected List<PointValueTime> getPointValuesExpected(List<Double> exepected) {
        PointValueTime oldValue = getOldValue();
        PointValueTime newValue = getNewValue();
        PointValueTime newValue2 = getNewValue2();

        List<PointValueTime> pointValuesExpected = new ArrayList<>();
        pointValuesExpected.add(oldValue);
        for(double i: exepected) {
            if(oldValue.getValue().equals(MangoValue.objectToValue(i))) {
                continue;
            }
            if(newValue.getValue().equals(MangoValue.objectToValue(i))) {
                pointValuesExpected.add(newValue);
            } else if(newValue2.getValue().equals(MangoValue.objectToValue(i))) {
                pointValuesExpected.add(newValue2);
            }
        }
        pointValuesExpected.sort(Comparator.comparing(PointValueTime::getTime).reversed());
        return pointValuesExpected;
    }

    protected List<PointValueTime> getPointValuesExpected() {
        PointValueTime oldValue = getOldValue();
        PointValueTime newValue = getNewValue();
        PointValueTime newValue2 = getNewValue2();
        List<PointValueTime> pointValuesExpected = new ArrayList<>();
        pointValuesExpected.add(oldValue);
        pointValuesExpected.add(newValue);
        pointValuesExpected.add(newValue2);
        pointValuesExpected.sort(Comparator.comparing(PointValueTime::getTime).reversed());
        return pointValuesExpected;
    }

    protected List<PointValueTime> getPointValuesWithUserExpected() {
        PointValueTime oldValue = getOldValueWithUser();
        PointValueTime newValue = getNewValueWithUser();
        PointValueTime newValue2 = getNewValueWithUser2();
        List<PointValueTime> pointValuesExpected = new ArrayList<>();
        pointValuesExpected.add(oldValue);
        pointValuesExpected.add(newValue);
        pointValuesExpected.add(newValue2);
        pointValuesExpected.sort(Comparator.comparing(PointValueTime::getTime).reversed());
        return pointValuesExpected;
    }

    protected void initValueByUser(DataPointRT dataPointRT) {
        dataPointRT.setPointValue(getOldValue(), getUser());
    }

    protected void initValue(DataPointRT dataPointRT) {
        dataPointRT.setPointValue(getOldValue(), null);
    }

    protected PointValueTime getOldValue() {
        return oldValue;
    }

    protected PointValueTime getOldValueWithUser() {
        return oldValueWithUser;
    }

    protected PointValueTime getNewValue() {
        return newValue;
    }

    protected PointValueTime getNewValueWithUser() {
        return newValueWithUser;
    }

    protected PointValueTime getNewValue2() {
        return newValue2;
    }

    protected PointValueTime getNewValueWithUser2() {
        return newValueWithUser2;
    }

    protected int getDataTypeId() {
        return dataTypeId;
    }

    protected String getDataType() {
        return dataType;
    }

    protected String getStartValue() {
        return startValue;
    }

    protected User getUser() {
        return user;
    }

    protected int getDefaultCacheSize() {
        return defaultCacheSize;
    }

    protected int getNumberOfLaunches() {
        return numberOfLaunches;
    }
}
