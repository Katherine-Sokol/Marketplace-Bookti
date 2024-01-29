import React from 'react';
import styles from './ResetPassword.module.scss'
import {Formik, Form, Field, ErrorMessage,} from 'formik';
import * as yup from 'yup';
import {LoginForm} from "../../../types";
import {useTranslation} from "react-i18next";
import Button from "../../Button/Button";
import axios from 'axios';
import {BASE_URL} from "../../../constants/api";
import {closeModal} from "../../../store/slices/modalSlice";
import {useAppDispatch} from "../../../hook";


// initialValues
const initialValuesEmail: LoginForm = {
    email: '',
}
const handleSubmit = async (values: LoginForm ) => {
    try {
        const response = await axios.post(`${BASE_URL}/authorize/login/resetPassword`, {
            values
        });

        console.log('Response:', response.data);
    } catch (error) {
        console.error('Error:', error);
    }
};
const ResetPassword = () => {
    const dispatch = useAppDispatch()
    const {t} = useTranslation(['login', 'modal']);

    const validationSchemaEmail: yup.Schema<LoginForm> = yup.object().shape({
        email: yup.string()
            .email(t('Error.login.email.email'))
            .required(t('Error.login.email.required')),
    })


    return (
        <div className={styles.Reset}>
            <h3 className={styles.ResetTitle}>{t('modal:ResetPassword.title')}</h3>
            <span className={styles.ResetText}>{t('modal:ResetPassword.text')}</span>
            <Formik initialValues={initialValuesEmail} validationSchema={validationSchemaEmail}
                    onSubmit={(values, {resetForm}) => {
                        handleSubmit(values)
                        resetForm()
                       dispatch(closeModal())
                    }}>
                <Form className={styles.ResetForm}>
                    <Field className={styles.ResetFormInput} type="email" name="email" placeholder={t('modal:ResetPassword.input')}/>
                    <ErrorMessage className={styles.ResetFormError} name="email" component="span"/>
                    <div className={styles.ResetFormButton}>
                        <Button name='ResetPasswordButton' type="submit">{t('modal:ResetPassword.button')}</Button>
                    </div>
                </Form>
            </Formik>
        </div>
    );
};

export default ResetPassword;